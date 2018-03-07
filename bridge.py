from py4j.java_gateway import JavaGateway
from py4j.java_collections import ListConverter

def processQueries(qlist):
    '''
        calls java-based lucene modules to return the relevant paragraphs for each query
        qlist is structured as such 'id|query|gold article' -> check example in main() below
    '''
    gateway = JavaGateway()
    jqlist = ListConverter().convert(qlist, gateway._gateway_client)
    return gateway.entry_point.processQueries(jqlist)

def main():
    # test code. should call this module only through processQueries(qlist)
    qlist = ["57210|Which pop group allowed Madonna to sing a cover of their song Gimme! Gimme! Gimme!|Madonna (entertainer)|paragraph", "57211|What is autism?|Autism|paragraph"]
    print(processQueries(qlist))

if __name__== "__main__":
    main()
