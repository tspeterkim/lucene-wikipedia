from py4j.java_gateway import JavaGateway
from py4j.java_collections import ListConverter, MapConverter

def processQueries(qlist):
    '''
        calls java-based lucene modules to return the relevant paragraphs for each query
        qlist is structured as such 'id|query|gold article|gold paragraph|gold answer(s)' -> check example in main() below
    '''
    gateway = JavaGateway()
    jqlist = ListConverter().convert(qlist, gateway._gateway_client)
    return gateway.entry_point.processQueries(jqlist)


def updateParams(config):
	'''
		updates hyperparameters of the lucene module
	'''
	gateway = JavaGateway()
	jconfig = MapConverter().convert(config, gateway._gateway_client)
	gateway.entry_point.updateParams(jconfig)


def test():
    # test code. In practice, should call this module only through processQueries(qlist)
    qlist = ["0|Which NFL team represented the AFC at Super Bowl 50?|Super Bowl 50|Super Bowl 50 was an American football game to determine the champion of the National Football League (NFL) for the 2015 season. The American Football Conference (AFC) champion Denver Broncos defeated the National Football Conference (NFC) champion Carolina Panthers 24–10 to earn their third Super Bowl title. The game was played on February 7, 2016, at Levi's Stadium in the San Francisco Bay Area at Santa Clara, California. As this was the 50th Super Bowl, the league emphasized the \"golden anniversary\" with various gold-themed initiatives, as well as temporarily suspending the tradition of naming each Super Bowl game with Roman numerals (under which the game would have been known as \"Super Bowl L\"), so that the logo could prominently feature the Arabic numerals 50.|Denver Broncos@@@Denver Broncos@@@Denver Broncos"]
    print(processQueries(qlist))
    updateParams({'context_a':'1'})
    print(processQueries(qlist))

if __name__== "__main__":
    test()
