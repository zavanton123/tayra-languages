#!/bin/sh
# One dump of the database into the bucket, then the oldest beyond KEEP_DUMPS removed.
set -eu
name="db/tayra-$(date -u +%Y%m%dT%H%M%SZ).dump"
s3() { aws --endpoint-url "$S3_ENDPOINT" --region "$S3_REGION" s3 "$@"; }
pg_dump --format=custom --no-owner | s3 cp - "s3://$S3_BUCKET/$name"
echo "Dumped the database to $name"
s3 ls "s3://$S3_BUCKET/db/" | awk '{print $4}' | grep '^tayra-.*\.dump$' | sort -r | tail -n +"$((KEEP_DUMPS + 1))" | while read -r old; do
  s3 rm "s3://$S3_BUCKET/db/$old"
done
