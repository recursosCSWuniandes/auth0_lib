/**
@param {object} user - The user being created
@param {string} user.id - user id
@param {string} user.tenant - Auth0 tenant name
@param {string} user.username - user name
@param {string} user.email - email
@param {boolean} user.emailVerified - is e-mail verified?
@param {string} user.phoneNumber - phone number
@param {boolean} user.phoneNumberVerified - is phone number verified?
@param {object} user.user_metadata - user metadata
@param {object} user.app_metadata - application metadata
@param {object} context - Auth0 connection and other context info
@param {string} context.requestLanguage - language of the client agent
@param {object} context.connection - information about the Auth0 connection
@param {object} context.connection.id - connection id
@param {object} context.connection.name - connection name
@param {object} context.connection.tenant - connection tenant
@param {object} context.webtask - webtask context
@param {function} cb - function (error, response)

module.exports = function (user, context, cb) {
  // Perform any asynchronous actions, e.g. send notification to Slack.
  var request = require('request');
  var EXTENSION_URL = "https://otroother14.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201";
  var TOKEN_URL = "https://otroother14.auth0.com/oauth/token";
  var CLIENT_ID = "6ivKAXVBdQAGd4dlL9BBOrOSFXc1Hlsn";
  var CLIENT_SECRET = "xn9aAaF929uGyHPXK7__b7YGcuKkw3ihbY0Vaz9ILPIaFV2sdD3q7iZ8G1MtfjrY";
  var GRANT_TYPE = "client_credentials";
  var AUDIENCE = "urn:auth0-authz-api";
  var ROLES = user.user_metadata.roles.replace("[","").replace("]","").split(",");
  var GROUP = user.user_metadata.group; 
  var USER_ID = user.id;
  
getAccessToken();

function getRoleId(roles,userId,token){
 
  var options ={
    method:"GET",
    url:EXTENSION_URL + "/api/roles",
    headers:{
      authorization:"Bearer "+ token
    }
  };
  request(options,function(error,response,body){
   
     if(!error && response.statusCode === 200){
      var rspRoles = JSON.parse(body).roles;
      var idRoles = [];
       for(var idx=0;idx<rspRoles.length;idx++){
         if(roles.indexOf(rspRoles[idx].name)>-1){
           idRoles.push(rspRoles[idx]._id);
         }
       }
       addRolesToUser(idRoles.join(),userId,token);
     }
  });
  
}
function addRolesToUser(roles,userId,token){
  var id = "auth0%7C" + userId;
  var options = {
    method: "PATCH",
    url:EXTENSION_URL + "/api/users/"+id+"/roles",
    headers:{
      authorization: "Bearer "+ token
    },
    json:[roles]
  };
  request(options,function(error,response,body){
    if(!error && response.statusCode === 204){
      console.log("");
      
    }
  });
}
function addUserToGroup(groupId,userId,token){
  var id = "auth0%7C" + userId;
  var options = {
    method:"PATCH",
    url:EXTENSION_URL + "/api/users/"+id+"/groups",
    headers:{
      authorization: "Bearer "+ token
    },
    json:[groupId]
  };
  request(options,function(error,response,body){
     if(!error && response.statusCode === 204){
       getRoleId(ROLES,userId,token)
      // 
     }
  });
}

function getGroupID(token){
  var options ={
        method:"GET",
        url:EXTENSION_URL+"/api/groups",
        headers:{
        authorization:"Bearer "+ token
        }};
        request(options,function(error,response,body){
          var groups = JSON.parse(body).groups;
      if(!error && response.statusCode === 200){
       for(var idx=0;idx<groups.length;idx++){
         if(groups[idx].name === GROUP){
           addUserToGroup(groups[idx]._id,USER_ID,token);
         }
       }
      }
});
}

function getAccessToken(){ 
  var options = {
    method:"POST",
      url: TOKEN_URL,
      headers:{
      "content-type": "application/json"
      },
      json:{
      "grant_type":GRANT_TYPE,
        "audience":AUDIENCE,
        "client_id":CLIENT_ID,
        "client_secret":CLIENT_SECRET
      }
    };
request(options,function(error,response,body){
      if(!error && response.statusCode === 200){
        TOKEN = body.access_token;
        getGroupID(body.access_token);
      }
    });
}

  cb();
};

*/
