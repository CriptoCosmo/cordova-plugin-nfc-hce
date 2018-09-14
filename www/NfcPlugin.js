var exec = require('cordova/exec');

var nfc = {

    readHce: function(aid, readCallback, errorCallback) {
        cordova.exec(readCallback, errorCallback, 'NfcPlugin', 'readHce', [aid]);
    },

    disableReaderMode: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'NfcPlugin', 'disableReaderMode', []);
    },

    switchToNFCSettings: function(){
		exec(null, null, "NfcPlugin", "switchToNFCSettings", []);
	},

    isNFCPresent = function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "NfcPlugin", "isNFCPresent", []);
    },

    isNFCEnabled: function(successCallback, errorCallback){
		exec(successCallback, errorCallback, "NfcPlugin", "isNFCEnabled", []);
    },
    
    isNFCAvailable: function(successCallback, errorCallback){
		exec(successCallback, errorCallback, "NfcPlugin", "isNFCAvailable", []);
    },
    
    onNFCStateChange: function(){},
};

module.exports = nfc;