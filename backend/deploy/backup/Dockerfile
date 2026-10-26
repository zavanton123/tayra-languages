# Dumps the database into the bucket every night, under db/, keeping the newest KEEP_DUMPS.
FROM postgres:18-alpine
RUN apk add --no-cache aws-cli
COPY dump.sh /usr/local/bin/dump.sh
CMD ["sh", "-c", "while true; do dump.sh || echo 'The dump failed; trying again tomorrow'; sleep 86400; done"]
