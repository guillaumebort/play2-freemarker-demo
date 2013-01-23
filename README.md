# Using freemarker as template engine with Play 2

While Play 2.0 comes with a really [powerful, clean and type-safe template engine](http://playframework.org/documentation/latest/JavaTemplates) that is both usable from Scala and Java, it is really simple to switch it to whatever else you want.

For example, for Java applications, you could want to use another well known template engine from the Java eco-system, such as [Freemarker](http://freemarker.sourceforge.net/).

> You can of course do the same for any other template engine library.

Here are the few steps needed.

## Adding the template engine dependency

Of course this is the first step. Really easy to do:

```scala
val appDependencies = Seq(
  javaCore,
  "org.freemarker" % "freemarker" % "2.3.19"
)
```

## Integrating the template files into the build system

Here it depends if your template files are compiled, or just processed at runtime. In most case you want to use a dynamic template engine, and the only thing you have to do is to embed the template files into the classpath, so they are available in the classpath both in _dev_ and _prod_ mode.

Here we will just copy the `app/views` directory content to the `target/.../classes/views`:

```scala
val main = play.Project(appName, appVersion, appDependencies).settings(
  
  unmanagedResources in Compile <<= (
    javaSource in Compile, 
    classDirectory in Compile, 
    unmanagedResources in Compile
  ) map { (app, classes, resources) =>
    IO.copyDirectory(app / "views", classes / "views", overwrite = true)
    resources
  }

)
```


## Using the template engine to feed the HTTP response

Here is the real integration between Play and your template engine. But wait... there is nothing to do. All the `ok(...)`, `notFound(...)`, etc. response generators take the response body as an argument.

The simplest way here is to have an helper method that takes the _template name_ and the _template arguments_, process them together and return an instance of `play.api.templates.Html`.

`play.api.templates.Html` is really nothing but a wrapper on top of a `String` that just allows the framework to auto-select the correct `Content-Type` response header. You could also build your own, altough it requires a bit of Scala trickery since the magic is based on [type classes](http://en.wikipedia.org/wiki/Type_class).

So, for freemarker, I have ended up with something like:

```java
// First import the helpers in your Controller
import static views.Freemarker.*;
...
// Then use it
public static Result index() {
  return ok(
    view("index.ftl",
      _("user", session("user")),
      _("products", Product.all())
    )
  );
}
```

## Integrating with the Logger infrastructure

Play 2 uses __slf4j__ as logging component. If your template engine library supports it, it is really straightforward to integrate with it. If it doesn't support it, slf4j has several bridges able to redirect other logging components.

Hopefully, __freemarker__ support it, so I can just configure it at the Java level using:

```java
static {
    try {
    freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_SLF4J); 
  } catch(Exception e) {
    throw new RuntimeException(e);
  }
}
```

And then, activate the proper level of logging in the Play `application.conf. file:

```properties
# Debug Freemarker
logger.freemarker=DEBUG
```

## Reporting errors properly

One of the coolest feature of Play is its awesome error reporting. If you integrate your own component it would be a shame to not report errors nicely. Hopefully it's really easy to do when you write a component for Play.

Here we can basically have 2 kind of errors.

## Template not found

Nothing really special here. You can't do really better than reporting a `RuntimeException` showing the source code trying to load the template. Play already do that automatically. We just hack the stacktrace a bit to place the error at the caller position:

```java
public static class TemplateNotFoundException extends RuntimeException {

  private final StackTraceElement[] callerStack;

  public TemplateNotFoundException(String template, StackTraceElement[] stack) {
    super("Template " + template + " is missing.");
    callerStack = new StackTraceElement[stack.length - 2];
    System.arraycopy(stack, 2, callerStack, 0, callerStack.length);
  }

  public StackTraceElement[] getStackTrace() {
    return callerStack;
  }

}
```

And here is the nice rendering:

![](https://raw.github.com/guillaumebort/play2-freemarker-demo/master/screenshots/template_notFound.png)

## Error in the template itself

If there is any error in the template parsing or rendering, it would be great to show the template source code in the browser, right?

Easy, just implement your own version of `PlayException.ExceptionSource` and throw it. Play will catch it and display it nicely:

```java
public static class ExceptionInTemplate extends play.api.PlayException.ExceptionSource {

  final String template;
  final Integer line;
  final Integer position;

  public ExceptionInTemplate(String template, Integer line, Integer position, String description, Throwable cause) {
    super("Freemarker error", description, cause);
    this.template = template;
    this.line = line;
    this.position = position;
  }

  public Integer line() {
    return line;
  }
  
  public Integer position() {
    return position;
  }
  
  public String input() {
    InputStream is = Play.application().resourceAsStream("/views/" + template);
    if(is != null) {
      try {
        StringBuilder c = new StringBuilder();
        byte[] b = new byte[1024];
        int read = -1;
        while((read = is.read(b)) > 0) {
          c.append(new String(b, 0, read));
        }
        return c.toString();
      } catch(Throwable e) {
        //
      }
    } 
    return "(source missing";
  }
  
  public String sourceName() {
    return template;
  }

}
```

And again, here is the nice rendering:

![](https://raw.github.com/guillaumebort/play2-freemarker-demo/master/screenshots/template_error.png)

## Hot reloading

If you are using a dynamic template engine it should be trivial to achieve. The build system being intregated with hot reloading, it will be incrementally called at each file change and new request events. 

If needed you can create your own implementation of `play.PlayPlugin` that will be notified of application __start__ and __stop__

For freemarker there nothing really special to do, apart of disabling the cache (at least in _dev_ mode):

```java
cfg.setTemplateUpdateDelay(0);
```

## Integrating with the __reverse router__

From within the templates, it's really useful to access the reverse router to generate the URL for links, based on the Java or Scala action call.

The reverse Router being just a set of statically compiled classes with static accessors, there is nothing special to do to integrate with. As soon as your template engine can call global static Java accessors, you can use the reverse router.

For freemarker, you have to inject a special model that act as a proxy in front of dynamically generated java calls:

```java
root.put("Router", new BeansWrapper().getStaticModels().get("controllers.routes"));
```

And then you can use it in your templates:

```html
<#import 'layout.ftl' as layout>

<@layout.main title="Home">

    <h2>${products ? size} Products</h2>

    <ul>
    <#list products as product>
        <li><a href="${Router.Application.product(product.id)}">${product.name}</a></li>
    </#list>
    </ul>

</@layout.main>
```

> Actually it would be the same to integrate with a few other components, such as the __i18n__ framework.

__Have fun!__

