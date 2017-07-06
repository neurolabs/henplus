#!/bin/bash

set -e

THISDIR=$(dirname $(readlink -f "$0"))

WORKDIR=$(mktemp -d)
trap "rm -rf $WORKDIR" 0 INT QUIT ABRT PIPE TERM

cd ${WORKDIR} && ${THISDIR}/debian/rules get-orig-source
cd ${WORKDIR} && tar xf *.orig.tar.xz
cd $(find ${WORKDIR} -mindepth 1 -type d) && dpkg-buildpackage -us -uc
cd $(find ${WORKDIR} -mindepth 1 -type d) && lintian || true
cd ${WORKDIR} && sudo dpkg -i *.deb
rm -rf $WORKDIR
