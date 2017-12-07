# Server

The **AMPII** project contains open source code for  implementations of the **BACnet/WS** (RESTful Web Services) protocol as defined in **ANSI/ASHRAE Standard 135-2016** *"BACnet® A Data Communication Protocol for Building Automation and Control Networks"*.

The "ampii-server" sub-project is the full Simulator/Server/Stack, implementing most every aspect of the protocol, including complex metadata and full support for security and fine-grained authorization.  It has some client functionality, but only enough to perform self tests. The list of things that is does *not* support is listed later in this file.

It is designed to serve up complex test data constructed from configuration files, or dynamic data from arbitrary sources using an extensible back-end "binding" mechanism. 

It is intended to be a reference implementation for testing and experimentation and doesn't claim to be production-ready code. The design emphasis is on simplicity, rapid development, and ease of debugging, not speed or size. However, some design decisions were made based on trying to minimize the in-memory footprint for testing large data sets, including a "sparse-list" mechanism for accessing back-end data sets that are too big to fit in memory.

It uses a minimum number of threads and was written with an emphasis on portability to other languages, so it does not leverage many of the esoteric features of Java (other than generics to avoid a lot of casting).

It also has *no dependencies on any external libraries*. None.  It only uses the standard JDK libraries.  The only significant thing that would need to be adapted to another language or environment would be the crypto stuff: TLS, signatures, etc.

Despite these disclaimers and design choices, it is covered by an "MIT License", so you can do whatever you want with it, even for commercial purposes.


Making and Running
------------------

`Application.java` contains an example main() that starts the server and takes a variety of command line options to avoid changing configuration files or code for setting operational parameters.

Project files for the free Community Edition of IntelliJ Idea IDE (version 2017-2.3) are included, but you can also just use the JDK command line tools, as follows.

Compiling:

    javac -sourcepath src -d out src/org/ampii/xd/application/Application.java
    
Running: 

    java -cp out org.ampii.xd.application.Application {command line arguments}


Configuring
-----------

The main configuration constants are in Application.java, e.g.,

    public static String   configFile      = "resources/config/config.xml";
    public static int      tcpPort         = 8080;
    public static int      tlsPort         = 4443;
    public static String   logDir          = "./logs";
    public static Locale   locale          = Locale.US;
    public static String   dataPrefix      = "/bws";
    public static int      thisDeviceInstance = 657780;

Almost all of these can be overridden with command line arguments.  In fact, it will scold you if you don't specify a --hostname, since the default "localhost" is only for testing and is meaningless in a real deployment.

Data Sources
------------

The server can be entirely driven by static data. It can load all of its data from XML or JSON configuration files at startup and then not save anything. This can be useful for repeatable/resettable configuration or to test with complex data data structures that a simple back-end source cannot provide.

In addition to the initial data provided by configuration files, every data node can be bound to a dynamic data source. The binding can provide not only a source/sink for live data, but can also define "policies" (i.e. what kind of metadata is supported) for that data. 

The initial data in the server is loaded via XML/JSON files specified by the
`Application.configFile` java constant (can be changed by the `--configFile` command line arg). Only the root and the "/.defs" node are created automatically, everything else (including TLS certificate/key) comes from the configuration files.

If you want to test factory default conditions, configure the user/pass to ".",
remove the TLS cert/key info, and set Application.autoactivateTLS to false.
This will then wait for external writes of that data before attempting to
start TLS.


Implemented Features
--------------------

Everything related to servers in the published standard 135-2016 and Addendum 135-2016bp is implemented here, except as noted below. (this is a loooong list of features, so it is not included in this document - go read the standard :-)

This code makes no attempt to allow multiple server instances in one JVM (as the use of static constants in the Application class indicates).

The XML parsing/generation is the minimum needed for conformance to the BACnet/WS standard. It does not support general namespaces or entities beyond the required ones (`&quot;` etc).

Most of the code tries to be just a generic-server-of-data without any
application-specific "behavior". All the things that are *not* generic are in
the "application" package. This includes, among others:
   - `BindingHooks.java`: this is where new ties to a back-end can be added.
   - `HTTPHooks.java`: this traps any special URI paths that are not normal data.
   - `PolicyHooks.java`: this can define what kind of data can be created.


Non-implemented Features
------------------------

- historyPeriodic() is just a shell for testing.  Since this is a "data digesting" function, the method for digesting large quantities of data  is almost always a platform specific operation. So historyPeriodic() expects to be overridden by backend-appropriate code with individual bindings or globally in  `historyHooks.java`.


- /.bacnet data:
    The data in the /.bacnet tree is "dead". It is not tied to any kind of
    communications back-end. There is also no local "behavior". e.g., writing Out Of Service to true doesn't make Present Value writable.

Known Issues / Bugs
-------------------

- This code implements the changes made by Addendum 135-2016bp. That addendum is still pending final publication. The last public review version is here: http://www.bacnet.org/Addenda/Add-135-2016bp-ppr1-draft-3_chair_approved.pdf. No substantive changes were made after the public review and it is currently awaiting final publication at ashrae.org.
- Root certificates: /.auth/root-cert-pend is not used to validate the /.auth/dev-cert-pend.

ToDo
----

There are several items that are planned but not completed yet.

- It would be useful to make console logs available through the web interface for remote testers.

- Client.java uses HttpURLConnection. It would be helpful for language portability to remove dependency on this library functionality and implement on raw sockets like the server side does.











