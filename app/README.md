### Remote Citrix / Token
### To Do
0. put services into separate classes to be called asynchronously

1. read google pub/sub configuration into configuration
4. fetch status from remote and show in supporting text, add listener
5. update status message and error status from within token publisher

## Program Flow
1. User is prompted to enter token, status message displays, enter 6-digit token
2. User enters 6-digit token, status message changes to token is valid
3. User submits token, status message changes to sending token to remote
4. Remote receives token, successfully authenticates, and sends back success
5. Or, Remote receives token, fails to authenticate, and sends back failure
6. Or, some other error, and sends back failure
7. Or, times out and the UI displays that status

Publishing the token is asynchronous as is receiving the status.

### Done
1. validate token input, 6 digits or limit to 6 digits
2. on click of submit, send to backend
3. capture status
4. implement Java / Kotlin Google pub/sub
    a. publish