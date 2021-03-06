Frequently Asked Questions
==========================

Resolved
--------

### How do I add default value in response header for specific end points?

-   It seems there is no existing filter to handle response header.
-   you can add headers to ResourceContext of specific endpoints
    implementation example as below:

*SampleHandlerImpl.java*

```
@Override
public Users getUsers(ResourceContext context, String ids) {
    List<String> idList = Arrays.asList(ids.split(","));
    List<User> userList = new ArrayList<User>();
    for (String id : idList) {
        User u = new User();
        u.setId(id);
        u.setName("name_" + id);
        userList.add(u);
    }

    // prepare response object
    Users users = new Users();
    users.setUsers(userList);

    // set cache control header 24h
    long expires = System.currentTimeMillis() + EXPIRES_IN_MS;

    context.response().setHeader("Cache-Control", "public");
    context.response().setDateHeader("Expires", expires);
    return users;
}
```

### How do I show detailed or backtrace log when server has errors?

-   For local Jetty, edit gradle.properties setting:

```
systemProp.log4j.debug=1
```

### How do I display *optional* or *required* for data object’s sub field in Swagger UI?

-   field “name” will be described with required, and the words after
    “//” will be displayed as the field’s description
-   click on “Model” link (located in “Data Type” column)

*sample.rdl*

```
type User struct {
    string name (x_size="min=3,max=5", optional); // desc for required name
    int32 age; // desc for optional age
}
```

### How do I enable filter for specific endpoint?

-   register filter to specific servlet whose name is “Web Application”

```
FilterRegistration.Dynamic fooFilter = context.addFilter("FooFilter", FOOFilter.class);
fooFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "Web Application");
fooFilter.setInitParameter("key", "val");
```

-   register filter to specific endpoint which url matches “/\*”

```
FilterRegistration.Dynamic fooFilter = context.addFilter("FooFilter", FOOFilter.class);
fooFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST) ,true,"/*");
fooFilter.setInitParameter("key", "val");
```

### How do I define different constraint rules in same data object for different endpoint?

Use "validation groups". Please refer to [Input Validation](/util/#input-validation) for details.


### Is it possible for one data object to extend another? 

- In RDL, you can define one type that include another as shown below:  
- In this RDL snippet, a type Foo is defined. And a type Bar include fields in type Foo, with one additional field `barField`.

```

type Foo struct {
   String  fooField1;
   String  fooField2;
   Int32   fooField3;
  
}


type Bar Foo {
    String    barField;
}

```

The generated Java classes of these two type doesn't preserve the "inheritance" relationship, but all fields from the "base" type `Foo` would be included in class `Bar`, as shown below:

```

public final class Foo implements java.io.Serializable {

    private String fooField1;    // (1)

    private String fooField2;    // (2)

    private int fooField3;       // (3)



    // unrelated code is omitted ...
}



public final class Bar implements java.io.Serializable {

    private String fooField1;    // (1)

    private String fooField2;    // (2)

    private int fooField3;       // (3)

    private String barField;     // (4)


    // unrelated code is omitted ...
}

```

Note that both classes have 3 common fields (`fooField1`, `fooField2` and `fooField3` ) however `Bar` doesn't extend from another.


### How do I define customized error layout?

-   define your error object in .rdl, and specify status code mapping to
    your custom data object

*sample.rdl*

```
type MyResourceError struct {
    string errorValue;
}

resource User GET "/user/{id}" {
    int32 id;

    expected ACCEPTED;
    exceptions {
        ResourceError INTERNAL_SERVER_ERROR;
        MyResourceError BAD_REQUEST;
        ResourceError UNAUTHORIZED;
        ResourceError FORBIDDEN;
    }
}
```

-   the custom error object and exception mapping of endpoint resources
    will be generated by rdl generator

*SampleResources.java (No need to implemented)*

```
@Path("/sample/v1")
public class SampleResources {

   @GET
   @Path("/user/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public User getUser(
       @PathParam("id") Integer id
   ) {
       try {
           ResourceContext context = _delegate.newResourceContext(_request, _response);
           User e = _delegate.getUser(context, id);
           return e;
       } catch (ResourceException e) {
           int code = e.getCode();
           switch (code) {
           case ResourceException.BAD_REQUEST:
               throw typedException(code, e, MyResourceError.class);
           case ResourceException.UNAUTHORIZED:
               throw typedException(code, e, ResourceError.class);
           case ResourceException.FORBIDDEN:
               throw typedException(code, e, ResourceError.class);
           case ResourceException.INTERNAL_SERVER_ERROR:
               throw typedException(code, e, ResourceError.class);
           default:
               System.err.println("*** Warning: undeclared exception ("+code+") for resource getUser");
               throw typedException(code, e, ResourceError.class);
           }
       }
   }
}
```

*MyResourceError.java (No need to implemented)*

```
public class MyResourceError {

   public String errorValue;

   public MyResourceError message(String errorValue) {
       this.errorValue = errorValue;
       return this;
   }

   public String toString() {
       return "{errorValue: \"" + errorValue + "\"}";
   }

}
```

-   you only need to throw ResourceException with your custom error
    object (the code must match with the object you defined in rdl)

```
public class SampleHandlerImpl implements SampleHandler {

   @Override
   public User getUser(ResourceContext context, Integer id) {
       User user = new User();
       user.setName("dm4");

       // throw your custom error object depends on your business logic
       if (id > 100) {
           MyResourceError e = new MyResourceError();
           e.setErrorValue(id + " is invalid");
           throw new ResourceException(ResourceException.BAD_REQUEST, e);
       }
       return user;
   }

   ...
}
```

-   the output will be

```
$ curl -v http://wifi-9-152.tpcity.corp.yahoo.com:8080/api/sample/v1/user/101

* Hostname was NOT found in DNS cache
*   Trying 10.82.9.152...
* Connected to wifi-9-152.tpcity.corp.yahoo.com (10.82.9.152) port 8080 (#0)
> GET /api/sample/v1/user/101 HTTP/1.1
> User-Agent: curl/7.37.1
> Host: wifi-9-152.tpcity.corp.yahoo.com:8080
> Accept: */*
>
< HTTP/1.1 400 Bad Request
< Date: Fri, 04 Sep 2015 02:13:18 GMT
< Content-Type: application/json
< Content-Length: 34
* Server Jetty(9.3.0.M2) is not blacklisted
< Server: Jetty(9.3.0.M2)
<
* Connection #0 to host wifi-9-152.tpcity.corp.yahoo.com left intact
{"errorValue":"101 is invalid"}
```

### How to change slf4j logger binding?

-   Parsec archetype generate build.gradle default binding implementation to
    log4j, you could change to logback or any other logger
    implementation what you like.
-   Below build.gradle example demo howto change binding to logback

```
dependencies {
    // ...
    compile group: 'ch.qos.logback', name: 'logback-classic', version: '1.1.9'
    // ...
}
```

-   Detail info could reference [slf4j
    doc](http://www.slf4j.org/manual.html#projectDep)


Pending
-------

### What does “public void authorize(String action, String resource, String trustedDomain)” mean (rdl generated code) and how do I use it?

### How do I define webapp root path?


### How do I define multipart in rdl?
