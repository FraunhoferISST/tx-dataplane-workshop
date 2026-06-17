#!/bin/sh

cd bruno-collection

npx --yes @usebruno/cli@3.5.0 run -r \
          '00_credential-issuance' \
          '01_dataplane-registration' \
          '02_provider' \
          --env="Kind - Local" \
          --env-var CLI=true
