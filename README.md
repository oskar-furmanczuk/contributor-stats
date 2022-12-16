___ABOUT___

<b> contributor-stats</b> lets you gather statistics about total number contributions done by each contributor for every organisation on GitHub.

___PREREQUISITES___

- Java 11 or higher
- SBT


___INITIALIZATION___

To start the app please run the following commands:
> sbt compile

and then


> sbt run [github-token]

where <b> github-token </b> is your personal access token generated on GitHub. Mandatory for high number of HTTP requests to GitHub API.

___USAGE___

To use the app open any HTTP client and make a GET request to 

http://localhost:8080/org/<organization-name>/contributors

where {organization-name} is any valid GitHub organization. 
Keep in mind that it won't be possible to calculate stats for enormous organizations like Microsoft or Apache. 


