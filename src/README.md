# CIMDBServer #
Implements ewb-grpc services and will output a Zepben CIM database.

To run insecurely:

    java -jar cimcap-1.0.jar
    
PKI authentication as well as Auth0 token based auth is supported. 
To run with the supplied test credentials:

    java -jar cimcap-1.0.jar -k test-creds/postbox.localdomain.key -c test-creds/postbox.localdomain.cert -a test-creds/cacert.pem -t 

Note the server is currently hardcoded to talk to zepben.au.auth0.com with https://evolve-ingestor/ as the audience.

