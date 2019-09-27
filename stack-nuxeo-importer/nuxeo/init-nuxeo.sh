#!/usr/bin/env bash
NUX_PACKAGES="nuxeo-web-ui nuxeo-jsf-ui nuxeo-platform-importer /training-test.zip"

if [[ -e ${NUXEO_HOME}/packages/configured-pkg ]]; then
  echo "# Refresh MP from cache"
  rm -rf /opt/nuxeo/server/packages/.packages
  nuxeoctl mp-install --relax=false --accept=true  /opt/nuxeo/server/packages/cache/*
else
  start=$(date --utc +%Y%m%d_%H%M%SZ)
  echo "# ${start}: Installing MP ${NUX_PACKAGES} ..."
  nuxeoctl mp-install ${NUX_PACKAGES} --relax=false --accept=true
  touch ${NUXEO_HOME}/packages/configured-pkg
  # use a cache because we cannot reinstall package from the store directory
  cp -ar /opt/nuxeo/server/packages/store/ /opt/nuxeo/server/packages/cache
  end=$(date --utc +%Y%m%d_%H%M%SZ)
  echo "# ${end}: MP installed"
fi
now=$(date --utc +%Y%m%d_%H%M%SZ)
cp ${NUXEO_CONF} "${NUXEO_DATA}/nuxeo-${now}.conf"
