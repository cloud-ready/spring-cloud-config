
FROM cirepo/java:oracle-8u171-jdk-alpine

VOLUME /tmp
VOLUME ["/root/.data"]

#ADD schema-mysql.sql /root/
ADD entrypoint.sh /root/
RUN chmod 755 /root/*.sh
ADD	*-exec.jar /root/
RUN touch /root/*-exec.jar


USER root
WORKDIR /root/
ENTRYPOINT ["/root/entrypoint.sh"]
CMD []
