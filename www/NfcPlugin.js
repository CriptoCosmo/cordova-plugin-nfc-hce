var exec = require('cordova/exec');

var nfc = {

    sendPayload: function(payload, readCallback, errorCallback) {
        cordova.exec(readCallback, errorCallback, 'NfcPlugin', 'sendPayload', [payload]);
    },

    readHceWithIntent: function(aid, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'NfcPlugin', 'readHceWithIntent', [aid]);
    },

    removeIntent: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'NfcPlugin', 'removeIntent', []);
    },

    readHce: function(aid, readCallback, errorCallback) {
        cordova.exec(readCallback, errorCallback, 'NfcPlugin', 'readHce', [aid]);
    },

    disableReaderMode: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'NfcPlugin', 'disableReaderMode', []);
    },

    switchToNFCSettings: function(){
		exec(null, null, "NfcPlugin", "switchToNFCSettings", []);
	},

    isNFCPresent: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "NfcPlugin", "isNFCPresent", []);
    },

    isNFCEnabled: function(successCallback, errorCallback){
		exec(successCallback, errorCallback, "NfcPlugin", "isNFCEnabled", []);
    },
    
    isNFCAvailable: function(successCallback, errorCallback){
		exec(successCallback, errorCallback, "NfcPlugin", "isNFCAvailable", []);
    },
    
    onNFCStateChange: function(){},
    onTagDiscoveredIntent: function(){},
    onCallStateChange: function(){}
};

module.exports = nfc;