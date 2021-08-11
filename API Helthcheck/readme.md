#We need to build a Health checker for HTTP based services that produces the following result when queried:
  Services where all instances are healthy - GREEN
  Services where only 50% instances are healthy - ORANGE
  Services where majority of the instances are not healthy- RED
A service instance is said to be healthy when, Health Checker has received at least 3 pings from the service instance in a minute, for the last 3 minutes.
Health Checker shall have an interface to register service instances and to accept pings from them.
It needs to have an interface to query status of all Services or status of all instances of a service.
Notes:
  Need to handle concurrency
  Different Hosts can ping data at the same time
  It goes without saying that - should Ignore pings from unregistered services
Notes
  Focus on building a working demo first. Demonstrable code is the primary expectation.
  You are free to use any programming language. You can write a CLI, a HTTP API, or even just methods that you call and demonstrate in a wrapper program.
  You will program on your own machine. Please ensure you have your IDE or development environment set up.
  You have access to documentation and all of the internet.
  Good code structure and modelling is a plus
