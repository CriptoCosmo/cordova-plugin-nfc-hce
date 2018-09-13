## NfcPlugin

#### Platforms 

- Android 


#### Installation

```shell
$ npm i cordova-plugin-fingerprintplugin
$ cordova plugin add cordova-plugin-fingerprintplugin
```
#### Remove

```shell
$ cordova plugin remove cordova-plugin-fingerprintplugin
```

#### Usage

```typescript
// HAS
var pin = "pinAutenticazione";
cordova.plugins.FingerprintPlugin.has(pin, (hasPin) => {
    console.log("success",hasPin);
    if (hasPin === 'OK') {
        veryfyFingerPrint();
    } else {
        console.log("pin non impostato");
    }
}, console.error);

// VERIFY 
function veryfyFingerPrint (){

    cordova.plugins.FingerprintPlugin.verify(pin, (pin) => {

        console.log("success",a);
        if(this.myInfo.pin == a){
            this.viewCtrl.dismiss({
                auth:true
            }); 
            this.statusBar.show();
        }else{
            console.log("Pin errato");
            this.errorMessage = "Pin errato";
        }

    }, (err) => {

        if(err === "onAuthenticationFailed"){
            console.log("Impronta non riconosciuta, Riprova";
            setTimeout(veryfyFingerPrint.bind(this),900));
        } else if (err.indexOf("onAuthenticationHelp") > -1){
            console.log(a.split("onAuthenticationHelp ")[1]);
            setTimeout(veryfyFingerPrint.bind(this),900);
        } else  if (err.indexOf("onAuthenticationError") > -1){
            console.log(err.split("onAuthenticationError ")[1]);
        }

    });
    
}

//SAVE 
var inputPin = "1234";
cordova.plugins.FingerprintPlugin.save(pin,inputPin,console.log,console.error);

//DELETE
cordova.plugins.FingerprintPlugin.delete(pin,console.log,console.error);

```

