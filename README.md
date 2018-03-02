# KFishHttp
## What's it?
Is a simple Http/s request Util and is on DEVELOPING
## How to Use it?
* Add this in Ur app/build.gradle  
`implementation 'cc.fish.kfishhttp:kfishhttp_lib:0.11-beta'`  
Or use the latest version  
`implementation 'cc.fish.kfishhttp:kfishhttp_lib:+'`  
  
* Use Requester In Kotlin  
``` 
Requester<T>().apply {
  url("$YOUR SERVER URL")
  mType = T::class.java
  get(context, T.success(), String.failed())
}
```
* In Java 8
```
Requester<T> req = new Requester<>();
req.url("API URL");
req.setMType(T.class);
req.get(this, tData -> {
  //TODO onSuccess
  return null;
},
s -> {
  //TODO onFailed
  return null;
});
```  
* In Java<=7
```
Requester<T> req = new Requester<>();
req.setMType(T.class);
req.url("API URL");
req.get(this, new Function1<T, Unit>() {
  @Override
  public Unit invoke(T tData) {
    //TODO onSuccess
    return null;
  }
}, new Function1<String, Unit>() {
  @Override
  public Unit invoke(String s) {
    //TODO onFailed
    return null;
  }
});
```
