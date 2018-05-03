package org.batfish.question.ipowners;

import static org.batfish.common.util.CommonUtil.computeIpInterfaceOwners;
import static org.batfish.question.ipowners.IpOwnersAnswerElement.COL_HOSTNAME;
import static org.batfish.question.ipowners.IpOwnersAnswerElement.COL_INTERFACE_NAME;
import static org.batfish.question.ipowners.IpOwnersAnswerElement.COL_IP;
import static org.batfish.question.ipowners.IpOwnersAnswerElement.COL_VRFNAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Map;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.Row;

class IpOwnersAnswerer extends Answerer {

  IpOwnersAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    IpOwnersQuestion question = (IpOwnersQuestion) _question;
    Map<String, Set<Interface>> interfaces =
        CommonUtil.computeNodeInterfaces(_batfish.loadConfigurations());
    Map<Ip, Map<String, Set<String>>> ipToInterface =
        computeIpInterfaceOwners(interfaces, question.getExcludeInactive());
    Map<Ip, Map<String, Set<String>>> ipOwners =
        CommonUtil.computeIpVrfOwners(question.getExcludeInactive(), interfaces);

    IpOwnersAnswerElement answerElement = new IpOwnersAnswerElement();

    answerElement.postProcessAnswer(
        _question, generateRows(ipOwners, ipToInterface, question.getDuplicatesOnly()));
    return answerElement;
  }

  @VisibleForTesting
  static Multiset<Row> generateRows(
      Map<Ip, Map<String, Set<String>>> ipOwners,
      Map<Ip, Map<String, Set<String>>> ipToInterface,
      boolean duplicatesOnly) {
    Multiset<Row> rows = HashMultiset.create();
    ipOwners.forEach(
        (ip, ownerMap) -> {
          int uniqueNodes = ownerMap.size();
          ownerMap.forEach(
              (node, vrfSet) -> {
                // Skip if returning only duplicates and no duplicates found, otherwise add row
                if (uniqueNodes * vrfSet.size() > 1 || !duplicatesOnly) {
                  vrfSet.forEach(
                      vrfName ->
                          rows.add(
                              Row.builder()
                                  .put(COL_HOSTNAME, node)
                                  .put(COL_VRFNAME, vrfName)
                                  .put(COL_INTERFACE_NAME, ipToInterface.get(ip).get(node))
                                  .put(COL_IP, ip)
                                  .build()));
                }
              });
        });
    return rows;
  }
}
