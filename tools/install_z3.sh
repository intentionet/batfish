#!/usr/bin/env bash
set -e

VERSION="2019-01-26-be61a26452735dfe62b14ddb4804ca5df0526dcf"

install_z3() {
  MAJOR_OS="$(major_os)"
  case "${MAJOR_OS}" in
    cygwin)
      install_z3_cygwin
      ;;
    linux)
      install_z3_linux
      ;;
    osx)
      install_z3_osx
      ;;
    *)
      echo "Unsupported operating system: ${MAJOR_OS}" 1>&2
      exit 1
      ;;
  esac
  echo "Successfully installed Z3!"
}

install_z3_cygwin() {
  WDIR="$(cygpath -u "${WINDIR}")"
  W32DIR="$WDIR/SysWOW64"
  W64DIR="$WDIR/System32"
  OLD_PWD="$PWD"
  Z3_REPO_URL="https://github.com/batfish/z3"
  Z364_ZIP_URL="${Z3_REPO_URL}/releases/download/z3-${VERSION}/z3-${VERSION}-x64-win.zip"
  Z364_ZIP="$(basename "${Z364_ZIP_URL}")"
  Z364_DIR="$(basename "${Z364_ZIP}" .zip)"
  Z332_ZIP_URL="${Z3_REPO_URL}/releases/download/z3-${VERSION}/z3-${VERSION}-x86-win.zip"
  Z332_ZIP="$(basename "${Z332_ZIP_URL}")"
  Z332_DIR="$(basename "${Z332_ZIP}" .zip)"
  set -x
  echo "Creating temporary installation folder for Z3"
  WORKING="$(mktemp -d)"
  cd "$WORKING"
  echo "Downloading Z3 64-bit from ${Z364_ZIP_URL}"
  wget "${Z364_ZIP_URL}"
  echo "Unpacking Z3 64-bit installation files"
  unzip "${Z364_ZIP}"
  echo "Installing Z3 64-bit to ${W64DIR}"
  cp "${Z364_DIR}/bin/libz3.dll" "${W64DIR}"
  getfacl "${W64DIR}/msvcr110.dll" | setfacl -f - "${W64DIR}/libz3.dll"
  cp "${Z364_DIR}/bin/libz3java.dll" "${W64DIR}"
  getfacl "${W64DIR}/msvcr110.dll" | setfacl -f - "${W64DIR}/libz3java.dll"
  cp "${Z364_DIR}/bin/libz3.dll" "${W64DIR}/z3.dll"
  getfacl "${W64DIR}/msvcr110.dll" | setfacl -f - "${W64DIR}/z3.dll"
  cp "${Z364_DIR}/bin/libz3java.dll" "${W64DIR}/z3java.dll"
  getfacl "${W64DIR}/msvcr110.dll" | setfacl -f - "${W64DIR}/z3java.dll"
  cp "${Z364_DIR}/bin/Microsoft.Z3.dll" "${W64DIR}/"
  getfacl "${W64DIR}/msvcr110.dll" | setfacl -f - "${W64DIR}/Microsoft.Z3.dll"
  echo "Downloading Z3 32-bit from ${Z364_ZIP_URL}"
  wget "${Z332_ZIP_URL}"
  echo "Unpacking Z3 32-bit installation files"
  unzip "${Z332_ZIP}"
  echo "Installing Z3 32-bit to ${W64DIR}"
  cp "${Z332_DIR}/bin/libz3.dll" "${W32DIR}/"
  getfacl "${W32DIR}/msvcr110.dll" | setfacl -f - "${W32DIR}/libz3.dll"
  cp "${Z332_DIR}/bin/libz3java.dll" "${W32DIR}/"
  getfacl "${W32DIR}/msvcr110.dll" | setfacl -f - "${W32DIR}/libz3java.dll"
  cp "${Z332_DIR}/bin/libz3.dll" "${W32DIR}/z3.dll"
  getfacl "${W32DIR}/msvcr110.dll" | setfacl -f - "${W32DIR}/z3.dll"
  cp "${Z332_DIR}/bin/libz3java.dll" "${W32DIR}/z3java.dll"
  getfacl "${W32DIR}/msvcr110.dll" | setfacl -f - "${W32DIR}/z3java.dll"
  cp "${Z332_DIR}/bin/Microsoft.Z3.dll" "${W32DIR}/"
  getfacl "${W32DIR}/msvcr110.dll" | setfacl -f - "${W32DIR}/Microsoft.Z3.dll"
  cd "${OLD_PWD}"
  echo "Removing temporary installation files"
  rm -rf "${WORKING}"
}

install_z3_linux() {
  DIST="$(linux_dist)"
  case "${DIST}" in
    ubuntu)
      install_z3_ubuntu
      ;;
    *)
      echo "Unsupported linux distribution: ${DIST}" 1>&2
      exit 1
      ;;
  esac
}

install_z3_osx() {
  ## for now, just install the libs in /usr/local/lib and /Library/Java/Extensions
  INSTALL_PREFIX="/usr/local"
  Z3_CACHE_DIR="${HOME}/.batfish_z3_cache"
  MD5_HASH="c2cebd313acc081346e3f8f4eb3eba93"
  BINDIR="${INSTALL_PREFIX}/bin"
  EXTDIR="/Library/Java/Extensions"
  LIBDIR="${INSTALL_PREFIX}/lib"
  OLD_PWD="${PWD}"
  OLD_UMASK="$(umask)"
  OSX_VERSION="10.14.2"
  Z3_REPO_URL="https://github.com/batfish/z3"
  Z3_ZIP_URL="${Z3_REPO_URL}/releases/download/z3-${VERSION}/z3-${VERSION}-x64-osx-${OSX_VERSION}.zip"
  Z3_ZIP="$(basename "${Z3_ZIP_URL}")"
  CACHED_Z3_ZIP="${Z3_CACHE_DIR}/${Z3_ZIP}"
  Z3_DIR="$(basename "${Z3_ZIP}" .zip)"
  set -x
  echo "Creating temporary installation folder for Z3"
  WORKING="$(mktemp -d)"
  cd "${WORKING}"
  umask 0022
  if [ -f "${CACHED_Z3_ZIP}" ]; then
    CURRENT_HASH="$(md5 ${CACHED_Z3_ZIP} | grep -o '[a-z0-9][a-z0-9]*$')"
  fi
  if [ "${CURRENT_HASH}" = "${MD5_HASH}" ]; then
    echo "Using cached Z3 zip stored at ${CACHED_Z3_ZIP}"
    cp "${CACHED_Z3_ZIP}" "${WORKING}/"
  else
    echo "Downloading Z3 from ${Z3_ZIP_URL}"
    wget "${Z3_ZIP_URL}"
    mkdir -p "$(dirname "${CACHED_Z3_ZIP}")"
    cp "${Z3_ZIP}" "${CACHED_Z3_ZIP}"
  fi
  echo "Unpacking Z3 installation files"
  unzip "${Z3_ZIP}"
  cd "${Z3_DIR}"
  echo "Installing Z3 to ${INSTALL_PREFIX}"
  mkdir -p "${LIBDIR}" "${EXTDIR}" "${BINDIR}"
  cp "lib/libz3.dylib" "lib/libz3java.dylib" "lib/libomp.dylib" "${LIBDIR}/"
  rm -f "${EXTDIR}/libz3.dylib"
  ln -s "${LIBDIR}/libz3.dylib" "${EXTDIR}/libz3.dylib"
  rm -f "${EXTDIR}/libz3java.dylib"
  ln -s "${LIBDIR}/libz3java.dylib" "${EXTDIR}/libz3java.dylib"
  cp "bin/z3" "${BINDIR}/"
  umask "${OLD_UMASK}"
  cd "${OLD_PWD}"
  echo "Removing temporary installation files"
  rm -rf "${WORKING}"
}

install_z3_ubuntu() {
  UBUNTU_VERSION="$(lsb_release -rs)"
  [ -n "${UBUNTU_VERSION}" ]
  INSTALL_PREFIX="/usr"
  Z3_CACHE_DIR="${HOME}/.batfish_z3_cache"
  BINDIR="${INSTALL_PREFIX}/bin"
  LIBDIR="${INSTALL_PREFIX}/lib"
  OLD_PWD="${PWD}"
  OLD_UMASK="$(umask)"
  declare -A MD5_HASH
  X64_1404="x64-14.04"
  X64_1604="x64-16.04"
  X64_1804="x64-18.04"
  MD5_HASH["${X64_1404}"]="d3d22fa5e6e95081e504c2f577d7767a"
  MD5_HASH["${X64_1604}"]="768fbd790c1921276f8522cfe5c421e8"
  MD5_HASH["${X64_1804}"]="a7c550356527eac59da384860ea29580"
  MACHINE="$(uname -m)"
  if [ "${MACHINE}" = "x86_64" ]; then
    ARCH="x64"
  elif [ "${MACHINE}" = "i386" ]; then
    ARCH="x86"
  else
    echo "Unsupported machine: ${MACHINE}"
    exit 1
  fi
  Z3_REPO_URL="https://github.com/batfish/z3"
  Z3_ZIP_URL="${Z3_REPO_URL}/releases/download/z3-${VERSION}/z3-${VERSION}-${ARCH}-ubuntu-${UBUNTU_VERSION}.zip"
  Z3_ZIP="$(basename "${Z3_ZIP_URL}")"
  CACHED_Z3_ZIP="${Z3_CACHE_DIR}/${Z3_ZIP}"
  Z3_DIR="$(basename "${Z3_ZIP}" .zip)"
  echo "Creating temporary installation folder for Z3"
  WORKING="$(mktemp -d)"
  cd "${WORKING}"
  if [ -f "${CACHED_Z3_ZIP}" ]; then
    CURRENT_HASH="$(md5sum ${CACHED_Z3_ZIP} | awk '{ print $1 }')"
  fi
  if [ "${CURRENT_HASH}" = "${MD5_HASH["${ARCH}"-"${UBUNTU_VERSION}"]}" ]; then
    echo "Using cached Z3 zip stored at ${CACHED_Z3_ZIP}"
    cp "${CACHED_Z3_ZIP}" "${WORKING}/"
  else
    echo "Downloading Z3 from ${Z3_ZIP_URL}"
    wget "${Z3_ZIP_URL}"
    mkdir -p "$(dirname "${CACHED_Z3_ZIP}")"
    cp "${Z3_ZIP}" "${CACHED_Z3_ZIP}"
  fi
  echo "Unpacking Z3 installation files"
  unzip "${Z3_ZIP}"
  cd "${Z3_DIR}"
  umask 0022
  echo "Installing Z3 to ${INSTALL_PREFIX}"
  cp "lib/libz3.so" "lib/libz3java.so" "${LIBDIR}/"
  cp "bin/z3" "${BINDIR}/"
  strip "${LIBDIR}/libz3.so"
  strip "${LIBDIR}/libz3java.so"
  strip "${BINDIR}/z3"
  umask "${OLD_UMASK}"
  cd "${OLD_PWD}"
  echo "Removing temporary installation files"
  rm -rf "${WORKING}"
}

linux_dist() {
  case "$(lsb_release -ds | awk '{print $1}')" in
    Ubuntu)
      echo ubuntu
      ;;
    *)
      echo unsupported
      ;;
  esac
}

major_os() {
  case "$(uname)" in
    *"CYGWIN"*)
      echo cygwin
      ;;
    Darwin)
      echo osx
      ;;
    Linux)
      echo linux
      ;;
    *)
      echo unsupported
      ;;
  esac
}

install_z3

