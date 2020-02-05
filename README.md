HenPlus
=======

**JDBC SQL-shell**

HenPlus is a SQL shell that can handle multiple open sessions in parallel. The commandline interface provides the usual history 
functions features and TAB-completion for commands, tables and columns.

In order to compile it, you need the java-readline in your classpath.

Overview
--------

HenPlus is a SQL shell written in Java that works for any database that offers JDBC support. So basically any database. 
Why do we need this ? Any database comes with some shell, but all of them have missing features (and several shells are simply 
unusable). And if you work with several databases at once (if you are a developer, then you do this all the time),
switching between these tools is tedious.

This is where *HenPlus* steps in. It supports:

* Any JDBC aware database.

* Has context sensitive command line completion for commands and tables / columns / variables within SQL-commands.

* Multiple open connection-sessions can be handled in parallel. You can just switch between them. So you can be connected to 
  different databases to experiment with the same statement in all of them, for example.

  All JDBC-Urls of opened sessions are stored for later command line completion in the `connect` command.

* Command line history just like in the bash (with cursor-up/down, CTRL-R, ...)

* A `describe` command for database tables, that work all JDBC-Drivers that unveil the appropriate MetaData (Oracle, PostgreSQL,
  MySQL, DB2...).

* Supports variables that are expanded like shell variables with $VARIABLENAME or ${VARIABLENAME} (with completion of their names,
  just like in bash.
 
* Loading files.
 
* Supports several built-in commands (`start`, `@`, `@@`, `spool`) and syntax from the Oracle SQL-plus utility (like the 
  single '/' on a line to close a statement). Most Oracle SQL-plus scripts will run directly, so its simple to switch to HenPlus. 
  Except if you can't stand, that your life will become much simpler, then ;-) If you have problems running your old scripts, 
  please let <a href="mailto:henplus@googlegroups.com">us</a> know.
 
* Allows a per-project storage of the commandline history, variables, plugins and connections.

* Allows to add your own plugins that behave just like the built-in commands.

* Is provided as Free Software. You are free to modify, copy, share and sell this software under the GPL. 
  See http://www.gnu.org/licenses/gpl.txt for more info.

History
-------

Know this ? You need a small little tool for your everyday work and
you wonder why nobody did this before -- and then you write it yourself.
HenPlus is the result of such an effort. Its name reflects the original 
intent: it started as a platform independent replacement for the 
Oracle SQLPlus tool. 

I wrote the first version of HenPlus in 1997 (before
Oracle was available for Linux) since I had to develop an application that 
connected to an Oracle database, but I had no Solaris machine
at home -- and the only SQLPlus I could use back then was running on some 
Redmond operating system that lacked (and still lacks) the 
environment to seriously develop software.

Since then I've rewritten this tool, added command line completion and
history -- but it still remained small and useful. It is freely 
distributable in source and binary form because it is protected by the 
GNU Public License. See http://www.gnu.org/licenses/gpl.txt for more info.

Getting started
---------------

### Download

You can download source and binary packages of HenPlus from the github page at http://github.com/neurolabs/henplus

As of July 2017, HenPlus is included in some distributions, for example
<a href="http://www.gentoo.org/">Gentoo</a>. On Debian and derivatives can try
the included build and install script. If you have all dependencies installed,
a simple:

    $ ./debian-install.sh

should build and install HenPlus (or complain about missing dependencies).

Use the respective package management tool to install HenPlus on these distributions.

For an installation on <a href="http://fink.sourceforge.net/">fink</a> for Mac have a look at 
<a href="http://blog.wannawork.de/index.php/2005/09/18/henplus_on_mac_os_using_fink">Bodo Tasche's blog</a>.

If you have a hard time to get the java-readline compiled on Mac OS X, then you might want to check out 
<a href="http://toonetown.blogspot.com/2006/07/java-readline-on-mac-os-x-update.html">Nathan Toone's blog</a> for an 
installation description.

### Compilation

Follow these instructions if you want to compile HenPlus yourself. First you need an additional library. HenPlus uses the 
features of the GNU-readline library and therefore needs the JNI java wrapper library.

<a href="http://sourceforge.net/project/showfiles.php?group_id=48669">java-readline 0.7.3</a>
( <a href="http://toonetown.blogspot.com/2006/07/java-readline-on-mac-os-x-update.html">compilation on Mac OS X</a> )

To build HenPlus the <a href="http://jakarta.apache.org/ant">ant build tool</a> (Version >= 1.4) is required. 

Now, just type

    $ ant jar

If you are root, then you can install it with:

    # ant install

which will install henplus in

`/usr/share/henplus/henplus.jar` The jar-file containing the HenPlus classes. You can add additional jar files in this directory. 
All of them are added to the classpath in the henplus shellscript (use this for JDBC-drivers).

`/usr/bin/henplus` Shellscript to start henplus.

If you want another installation base (default: `/usr`), you provide this with the parameter `prefix`:

    $ ant -Dprefix=/usr/local install

(For package providers: the build.xml provides as well the `DESTDIR` parameter with a similar meaning as in usual Makefiles).

If you've created packages for other operating systems (like Solaris) or Windows, please 
<a href="mailto:henplus@googlgroups.com">let us know</a>.

We haven't compiled this on Windows, but it shouldn't be a big deal if you manage to compile the java-readline. This is an 
JNI shared library and thus platform dependant. If you did it, please post your experience, so that we can include it in 
this documentation.

Running
-------

You can start HenPlus with the `henplus` shell script with or without an jdbc-url on the command line.

    $ henplus jdbc:mysql://localhost/foobar

### Make sure, command line editing is enabled

If the first line, henplus writes reads:

    no readline found (no JavaReadline in java.library.path). Using simple stdin.

then, the JNI-part of the readline library could not be found, so command line editing is disabled because henplus then reads
from stdin as fallback. This happens if the `LD_LIBRARY_PATH` does not point to the JNI library; edit the `/usr/bin/henplus`
shellscript so that the `LD_LIBRARY_PATH` contains the directory where `libJavaReadline.so` resides.

For RedHat and Debian you can just install the `libreadline-java` package.

### First steps

The important commands you need to know to get it running are `help` and `connect`. The help command gives an overview, 
what commands are supported:

    Hen*Plus> help
     help | ?                                 : provides help for commands
     about | version | license                : about HenPlus
     exit | quit                              : exits HenPlus
     echo | prompt                            : echo argument
     list-plugins | plug-in | plug-out        : 
    handle Plugins
     list-drivers | register | unregister     : 
    handle JDBC drivers
     list-aliases | alias | unalias           : 
    handle Aliases
     list-key-bindings | bind-key-cmd-string  : 
    handle function key bindings
     load | start | @ | @@                    : load file and execute commands
     connect | disconnect | rename-session | switch | sessions: 
    manage sessions
     status                                   : show status of this connection
     tables | views | rehash                  : list available user objects
     describe <tablename>                     : describe a database object
     tree-view <tablename>                    : 
    tree representation of connected tables
     dump-out | dump-in | verify-dump | dump-conditional: 
    handle table dumps
     system | !                               : execute system commands
     set-var | unset-var                      : 
    set/unset variables
     set-property | reset-property            : 
    set global HenPlus properties
     set-session-property | reset-session-property: 
    set SQL-connection specific properties
    
    config read from [/home/hzeller/.henplus]

You exit the HenPlus shell by typing the `exit` or `quit` command or by just typing EOF-Character (CTRL-D).

For almost all commands, there is more detailed help available. You can just explore the commands by typing `help [commandname]`
and learn what the built-in commands are all about; all help entries have a synopsis and a more detailed description like in 
the following example:

    Hen*Plus> help system
    
    SYNOPSIS
            system <system-shell-commandline>
    
    
    DESCRIPTION
            Execute a system command in the shell. You can only invoke
            commands,  that do  not expect  anything  from  stdin: the
            interactive  input  from HenPlus  is disconnected from the
            subprocess' stdin. But this is useful to call some small
            commands in the middle of the session. There are two syntaxes
            supported: system <command> or even shorter with the
            exclamation mark: !<command>.
            Example:
            !ls

But for now, we are interested how to get connected, so start with the `connect` command

    Hen*Plus> help connect

Getting connected
-----------------

Getting connected to a database is simple: you need the JDBC driver from your database vendor, put it in the classpath, 
register the driver -- that's it. HenPlus provides the commands `register`, `unregister` and `list-drivers` to manage the drivers 
(you know it already: the `help` command tells you more details).

### Put the JDBC-Driver in the CLASSPATH

The JDBC-Driver must be in the classpath to be used by HenPlus. Therefore, you can either add it to the Classpath manually:

    $ CLASSPATH=$CLASSPATH:/path/to/my/driver.jar
    $ export CLASSPATH
    $ henplus

The simplest way, however, is to put the jar file with the driver in the `/usr/share/henplus` directory:

    # cp /path/to/my/driver.jar /usr/share/henplus
    $ henplus

Since you need to be root to copy the jar-file to that directory, it is as well possible to add the driver to the 
`.henplus/lib` directory in your home-directory

    $ mkdir -p ~/.henplus/lib
    $ cp /path/to/my/driver.jar ~/.henplus/lib

If you use different `.henplus` directories for the per project configuration (see below), you need to put the 
driver in that directory.

### Register the driver

By registering the driver, you allow HenPlus to associate the JDBC-Urls with the appropriate JDBC-Drivers.

Some of the common drivers are already registered by default. To see, what drivers are registered and which of them are 
actually found in the CLASSPATH, use the `list-drivers` command.

    Hen*Plus> list-drivers
    loaded drivers are marked with '*' (otherwise not found in CLASSPATH)
    ------------+---------------------------------+---------------------------------------+
        for     |          driver class           |              sample url               |
    ------------+---------------------------------+---------------------------------------+
       Adabas   | de.sag.jdbc.adabasd.ADriver     | jdbc:adabasd://localhost:7200/work    |
       DB2      | COM.ibm.db2.jdbc.net.DB2Driver  | jdbc:db2://localhost:6789/foobar      |
     * MySQL    | org.gjt.mm.mysql.Driver         | jdbc:mysql://localhost/foobar         |
     * Oracle   | oracle.jdbc.driver.OracleDriver | jdbc:oracle:thin:@localhost:1521:ORCL |
     * Postgres | org.postgresql.Driver           | jdbc:postgresql://localhost/foobar    |
       SAP-DB   | com.sap.dbtech.jdbc.DriverSapDB | jdbc:sapdb://localhost/foobar         |
    ------------+---------------------------------+---------------------------------------+

With the `list-drivers` command you can see what driver classes are registered and loaded. If you cannot connect to some JDBC URL, 
check first, if the appropriate driver is actually loaded. If it is not loaded, then it is probably not found in the
CLASSPATH (see above).

Drivers once registered are remembered by HenPlus, so that they are loaded automatically on next startup. For 
registering and unregistering drivers, see the online help:

    Hen*Plus> help register
    Hen*Plus> help unregister

### Connect

When the driver is loaded, you can connect to the database using the JDBC-URL:

    Hen*Plus> connect jdbc:oracle:thin:@localhost:1521:ORCL
    HenPlus II connecting
     url 'jdbc:oracle:thin:@localhost:1521:ORCL'
     driver version 1.0
    ============ authorization required ===
    Username: henman
    Password: 
     Oracle - Oracle8i Enterprise Edition Release 8.1.7.0.1 - Production
    JServer Release 8.1.7.0.1 - Production
     read committed *
     serializable
    henman@oracle:localhost>

This will then ask for the username and the password and you are connected. Since it is not possible to set the terminal to 
non-echo mode while typing the password, a thread constantly redraws the prompt (This is after a hack 
<a href="http://java.sun.com/features/2002/09/pword_mask.html">found here</a>; thanks so Alec Noronha for the link.)
If the the redrawing causes trouble with your installation, please let <a href="mailto:henplus@googlegroups.com">us</a> know).

Note, that many JDBC-Drivers allow for a URL-syntax, that already includes the name and the password; in that case, you can connect
directly without typing the user/password; for example in Oracle:

    Hen*Plus> connect jdbc:oracle:thin:
    foo/bar@localhost:1521:BLUE
    HenPlus II connecting
     url 'jdbc:oracle:thin:foo/bar@localhost:1521:BLUE'
     driver version 1.0
     Oracle - Oracle9i Enterprise Edition Release 9.2.0.1.0 - Production
    With the Partitioning option
    JServer Release 9.2.0.1.0 - Production
     read committed *
     serializable
    FOO@oracle:localhost>

Typing JDBC-URLs is tedious ? That's right, so HenPlus remembers all the connection URLs you were connected to and provides 
it in the context sensitive commandline completion for the connect command. So next time you connect, you just type

    Hen*Plus> connect 
    <TAB>
    jdbc:oracle:thin:@localhost:1521:ORCL  jdbc:oracle:thin:@database.my.net:1521:BLUE
    Hen*Plus> connect jdbc:oracle:thin:@
    <TAB>
    Hen*Plus> connect jdbc:oracle:thin:@database.my.net:1521:BLUE

... and connecting with long URLs is a piece of cake.

### Aliases 

On connect, you can provide a short name for that session as second parameter after the JDBC-URL:

    Hen*Plus> connect jdbc:oracle:thin:@database.my.net:1521:BLUE myora
     [.. connect to jdbc:oracle:thin:@database.my.net:1521:BLUE ..]
    myora>

This will do two things: first the session name will be renamed after this alias and this name can be used as a shortcut for 
later connections:

    Hen*Plus> connect myora
     [.. connect to jdbc:oracle:thin:@database.my.net:1521:BLUE ..]
    myora>

On connection, the prompt changes to a string that reflects the current connection. By default, this prompt is automatically 
extracted from the JDBC-URL, but you can provide another name as second parameter in the connection command (see
help for the `connect` command). Or just rename the session:

    henman@oracle:localhost> rename-session hello
    hello>

You can disconnect with the `disconnect` command or by pressing CTRL-D.

Special Characters in the Shell
-------------------------------

### Command Separators

Usually there is one command per line. However, you can have multiple
commands on one line if you separate them with a semicolon:

    echo "*** The build-in help ***" ; help

The SQL commands however (you guess it: `select`, `update`, `create` .. ) are not complete after the newline; you *always*
have to close them with a semicolon -- so it is possible to write statements on multiple lines:

    oracle:localhost> create table foobar (
                        id number(10) primary key,
                        text varchar(127)
                      );
    ok. (70 msec)
    oracle:localhost>

Some commands are not even complete, if there is a semicolon -- these are `create procedure` and `create trigger`. 
These commands contain some more complex SQL-operations that are each separated by a semicolon. The whole
command is then completed with a single slash at the beginning of a new line (this syntax is the same that SQLPlus supports):

    oracle:localhost> create or replace trigger foobar_autoinc
                      before insert on foobar
                      for each row
                      begin
                        select foobar_seq.nextval into :new.id from dual;
                      end foobar_autoinc;
                      /
    ok. (320 msec)
    oracle:localhost>

(The current keywords that trigger requiring the `/` as command separator are CREATE followed by PROCEDURE, TRIGGER, 
FUNCTION or PACKAGE later. If this  is not enough, please let us know).

### SQL-Comments

That should be enough to start working with HenPlus. But especially if you are running scripts (with the `load`-command)
there is one additional piece of information you might need to know: the types of comments that are ignored. AFAIK, the
SQL standard defines only an ANSI-endline comment, that starts with two dashes; this is supported by HenPlus:

    select * from foobar; -- this is a comment

However, other non-standard types of comments have come to use in several SQL-shells so HenPlus ignores these as well. One style 
are the C/C++/Java style comments, that comment out a range between `/* some comment */`.

    /*
       This is a longer comment that goes
       across several lines
     */
     select * from foobar;

Another style of comments, allowed for instance in the MySQL-Shell is the UNIX-shell like '#' endline comment; however, this 
character is *only* allowed as first character of a line to be a comment -- otherwisemusing the Hash-Symbol in normal 
SQL-Statements (e.g. column names), would not work.

    # this is a comment.
     select * from foobar;
     create table foo (id# number); -- the hash here does not start a comment

One beta tester requested an additional type of comment: Two semicolons at *the beginning* of a line to comment out the whole 
line. The problem with semicolon is, that it is as well used as separator between commands and as such may of course occur 
twice in a row. Therefore, two semicolons are *only* regarded as comment, if they are the first on a line:

    ;; This line is commented out
       ;; this one as well, since there are only whitespaces on the left
    select * from foobar ;; echo "this echo is executed as usual"

To make a long story short -- the supported types of comments are

* Ansi-Style `--` (two dashes) comment until end of line
* UNIX-Shell Style `#` comment until end of line
* Lisp-like `;;` comment until end of line
* C/C++/Java like `/* range comment */

*Not* supported is the C++/Java like endline comment that starts with two slashes `//`. The reason is, that many JDBC-URLs contain
these two slashes, and we don't want to comment these out, right ? ;-)

Comment removal can be switched off. Usually, all comments are removed in HenPlus before sending commands to the database, 
because many JDBC-drivers or databases cannot handle comments sufficiently. However, sometimes it is necessary *not* to remove 
comments, since some databases use comments to convey hinting in statements. In Oracle, for instance, you give hints to the 
query optimizer in the form

    select /*+ index(foo,foo_a_idx) */ a from foo where ...

For this reason, it is possible to switch off automatic comment removal. This is the global HenPlus property `comments-remove`; 
it allows to switch off comment removal, so strings are sent to the database as-is. The command is 

    set-property comments-remove off

Multiple Open Sessions
----------------------

You can be connected to multiple databases at once; just issue the `connect` command multiple times. You can list the sessions 
you are connected to the `sessions` command. Of course, the shell provides only access to one session at a time so you can switch 
between the sessions with the `switch` command.

    Hen*Plus> connect jdbc:oracle:thin:@localhost:1521:ORCL
    
    [... enter user/password ...]
    henman@oracle:localhost> connect jdbc:mysql://localhost/test
    mysql:localhost> sessions
    current session is marked with '*'
    ------------------------------+--------+---------------------------------------+
               session            |  user  |                 url                   |
    ------------------------------+--------+---------------------------------------+
        henman@oracle:localhost   | henman | jdbc:oracle:thin:@localhost:1521:ORCL |
      * mysql:localhost           | [NULL] | jdbc:mysql://localhost/test           |
    ------------------------------+--------+---------------------------------------+
    mysql:localhost> switch henman@oracle:localhost
    henman@oracle:localhost>

Of course, the switch command provides command line completion for the other sessions names. If you only have two sessions, 
then its even easier: just type `switch` without parameter (so in the example above, this would have been sufficient).

Using Variables
---------------

### Manual use of Variables

You can use Variables, that can be used as text replacement everywhere. The replacement works similar to shellscripts, however 
setting must be done explicitly with the `set` command instead of a simple assignment:

    henman@oracle:localhost> set-var tabname footab
    henman@oracle:localhost> select count(*) from ${tabname};

*Note:* The command for setting variables has changed since Version 0.9 of HenPlus. Previously, this command was `set`; it 
changed to `set-var`, since several Databases understand `set` as a build in-command that otherwise would be shadowed by 
HenPlus' `set`.

Setting variables with values that span multiple lines have to be embraced by single or double-quotes; in the next example we 
use a variable to define storage options once and reuse it for multiple tables in Oracle (note the quotes):

    henman@oracle:localhost> set-var STORAGE_OPTIONS  "pctused 40 pctfree 10
             storage ( initial 512k NEXT 1024k PCTINCREASE 0
                       MINEXTENTS 1 MAXEXTENTS UNLIMITED )
             TABLESPACE users"
    henman@oracle:localhost> create table foo (id number(10), text varchar2(1024)) ${STORAGE_OPTIONS};
    henman@oracle:localhost> create table bar (id number(10), description varchar2(1024)) ${STORAGE_OPTIONS};

Variables can be expanded with or without curly braces: $FOO and ${FOO} is the same.

If you don't want a variable to be expanded, double the dollar-sign: `$$FOO`; the Unix-shell way to embed it in single-quotes 
(like `$FOO`) does *not* work.

Unlike the shell, variables that are *not* set, are *not* expanded to an empty string, but left as they are. So an unset variable 
`$FOOBAR` expands to .. `$FOOBAR`. This is, because some databases (especially Oracle in system views) use names with dollar 
characters; thus character sequences starting with '$' that cannot be recognized as HenPlus-variable have to be left as they are.

### Special Variables

Special variables are variables that have a special meaning to HenPlus. Currently there is only one special variable, that is set 
automatically.

`_HENPLUS_LAST_COMMAND` This variable contains the last command executed by HenPlus

### Showing and Unsetting Variables

All variable settings can be shown with the `set-var` command without any parameters. Unsetting variables works with `unset-var`.

    henman@oracle:localhost> set-var
    -----------------------+------------------------------------------------------------------------------------+
             Name          |                                       Value                                        |
    -----------------------+------------------------------------------------------------------------------------+
     STORAGE_OPTIONS       | pctused 40 pctfree 10                                                              |
                           |          storage ( initial 512k NEXT 1024k PCTINCREASE 0                           |
                           |                    MINEXTENTS 1 MAXEXTENTS UNLIMITED )                             |
                           |          TABLESPACE users                                                          |
     _HENPLUS_LAST_COMMAND | create table bar (id number(10), description varchar2(1024)) pctused 40 pctfree 10 |
                           |          storage ( initial 512k NEXT 1024k PCTINCREASE 0                           |
                           |                    MINEXTENTS 1 MAXEXTENTS UNLIMITED )                             |
                           |          TABLESPACE users;                                                         |
     tabname               | footab                                                                             |
    -----------------------+------------------------------------------------------------------------------------+
    henman@oracle:localhost> unset-var STORAGE_OPTIONS tabname
    henman@oracle:localhost> set-var
    -----------------------+-----------------------------------+
             Name          |               Value               |
    -----------------------+-----------------------------------+
     _HENPLUS_LAST_COMMAND | unset-var STORAGE_OPTIONS tabname |
    -----------------------+-----------------------------------+

All non-special variables that are set at the end of the session are stored, so that they are available on next startup.

Setting Properties
------------------

There are some global properties, that can be modified by the `set-property` command. Connection session specific properties are 
handled with the `set-session-property` command. Corresponding `reset-*` commands reset the property to its default.

The usage of these commands is simple. If you just type the set-* command, then the list of supported properties with short 
description is shown. With the property name given as single parameter, the detailed help for that property is given. With an 
additional parameter the property is actually set to that value:

    sa@hsqldb> set-property                      -- no parameters: show list
    -----------------------+-------+-------------------------------------------------------+
             Name          | Value |                      Description                      |
    -----------------------+-------+-------------------------------------------------------+
     column-delimiter      | |     | modify column separator in query results              |
     comments-remove       | on    | switches the removal of SQL-comments                  |
     echo-commands         | off   | echo commands prior to execution.                     |
     sql-result-limit      | 2000  | set the maximum number of rows printed                |
     sql-result-showfooter | on    | switches if footer in selected tables should be shown |
     sql-result-showheader | on    | switches if header in selected tables should be shown |
    -----------------------+-------+-------------------------------------------------------+
    sa@hsqldb> set-property column-delimiter  -- property name parameter: show detailed description
    
    DESCRIPTION
            Set another string that is used to separate columns in
            SQL result sets. Usually this is a pipe-symbol '|', but
            maybe you want to have an empty string ?
    sa@hsqldb> set-property column-delimiter "****"       -- property name and value: set property

Note, that the property names are TAB-completed and even the values, if they are restricted (like 'on', 'off').

### Global Properties

Global properties are stored in your project preferences. so that you don't have to retype them on next startup.

    sa@hsqldb> select * from FOO ;
    ---+---+---+
     X | Y | Z |
    ---+---+---+
     1 | 2 | 3 |
    ---+---+---+
    1 row in result (first row: 4 msec; total: 7 msec)
    sa@hsqldb> set-property column-delimiter "****"       -- property name and value: set property
    sa@hsqldb> select * from FOO ;
    ------+------+------+
     X **** Y **** Z ****
    ------+------+------+
     1 **** 2 **** 3 ****
    ------+------+------+
    1 row in result (3 msec)
    sa@hsqldb> reset-property column-delimiter

### Session Properties

For supported properties of the SQL-connection session just type `set-session-property`. At present, the properties 
`auto-commit`, `isolation-level` and `read-only` are supported. Unlike the global properties, this set of properties is specific 
per session and is not stored.

The values that are possible for the transaction isolation are provided by the
JDBC-driver and are conveniently provided by the TAB-completion:

    henman@oracle:localhost> set-session-property
    -----------------+----------------+-----------------------------------------------+
          Name       |     Value      |                  Description                  |
    -----------------+----------------+-----------------------------------------------+
     auto-commit     | off            | Switches auto commit                          |
     isolation-level | read-committed | sets the transaction isolation level          |
     read-only       | off            | Switches on read only mode for optimizations. |
    -----------------+----------------+-----------------------------------------------+
    henman@oracle:localhost> set-session-property i<TAB>
    henman@oracle:localhost> set-session-property isolation-level <TAB><TAB>
    read-committed  serializable
    henman@oracle:localhost> set-session-property isolation-level s<TAB>
    henman@oracle:localhost> set-session-property isolation-level serializable
    henman@oracle:localhost> set-session-property
    -----------------+--------------+-----------------------------------------------+
          Name       |    Value     |                  Description                  |
    -----------------+--------------+-----------------------------------------------+
     auto-commit     | off          | Switches auto commit                          |
     isolation-level | serializable | sets the transaction isolation level          |
     read-only       | off          | Switches on read only mode for optimizations. |
    -----------------+--------------+-----------------------------------------------+

Commandline Editing/Completion
------------------------------

HenPlus provides the common amenities for command line editing and history (cursor-up/down, CTRL-R) that are already known from 
bash (See the <a href="http://www.faqs.org/docs/bashman/bashref_81.html">Bash Command Line Editing</a> documentation for details).

In addition to that, HenPlus provides TAB-completion for virtually everything that can be completed according to the context: 
tables in select statements, column-names in where-clauses, variable-names in set-var/unset-var command, variable names after 
typing `$` or `${'`, Function Key Names in the bind-key-cmd command, property names and its values... just try it.

Per Project Configuration
-------------------------

HenPlus stores almost any settings like the commandline history, the connection-URLs, variable settings, parameters, aliases,
key bindings etc. in files in a directory in the filesystem. This directory is by default located in your home directory and
called `~/.henplus`. Whenever you start `henplus`, this configuration directory is used by default.

If you have multiple projects you work on, the settings of all these projects would collected in the `~/.henplus` directory, thus
your command history would fill up with commands not needed in that project or you need switch settings all the time.
So if you work in multiple projects with specific connect strings, variables, properties etc. a per project separation of this 
information is desirable.

HenPlus supports per project storage of this information.

To store only information you use in some project, just create an empty `.henplus` directory in the directory you usually start the
HenPlus utility -- then HenPlus will use this directory to store its configuration. More, if it does not find the `.henplus`
directory within the current working dir, it goes up the directory tree until it finds the `.henplus` directory. If it still does
not find the directory, it falls back to the `~/.henplus` in your home directory.

As an example consider these directories:

    /home/hzeller/.henplus/
    /home/hzeller/projects/project-a/.henplus/
    /home/hzeller/projects/project-b/.henplus/

Starting HenPlus in any directory below `/home/hzeller/projects/project-a/` will use `/home/hzeller/projects/project-a/.henplus/`
configuration directory, same for any directory below `/home/hzeller/projects/project-b/` for instance 

    $ cd /home/hzeller/projects/project-b/src/henplus/commands
    $ henplus
    using GNU readline (Brian Fox, Chet Ramey), Java wrapper by Bernhard Bablok
    henplus config at 
    /home/hzeller/projects/project-b/.henplus
     [...]

Starting henplus anywhere else will use the configuration in the home directory in `/home/hzeller/`

If you type `help`, then HenPlus tells you in the last line, where it has loaded its initial configuration from.

Plugins
-------

Need some command that creates some fancy statistics or report from your database tables ? Or need a command that you are missing
from some other database tool? Or want to pop up some Swing-window to do database record editing?

HenPlus does not limit you to the features already provided. It is very simple to write plug-ins, that can be added and removed 
at run time. See the help for `plug-in`, `plug-out` and `list-plugins` for details. Basically, you just need to write a class 
that implements the `henplus.Command` interface. A plugin will register one (or a set of) new commands that behave just like 
internal commands. They are, for instance, available in the `help` command.

There is one cool plugin shipped since version 0.9.4, provided by <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>.
It resides in the henplus.jar, so if you have installed henplus, it is already there, but you have to plug-in this command 
yourself; after plugging it in, HenPlus will remember this for future starts.

    Hen*Plus> plug-in henplus.plugins.tablediff.TableDiffCommand
    adding commands: tablediff
    Hen*Plus> list-plugins
    loaded plugins are marked with '*'
    ----------------------------------------------+-----------+
                     plugin class                 | commands  |
    ----------------------------------------------+-----------+
     * henplus.plugins.tablediff.TableDiffCommand | tablediff |
    ----------------------------------------------+-----------+

What does this command do ? By providing two session names and a list of tables, you get the meta difference for corresponding 
tables in both sessions. So you get whether columns have been added/removed or datatypes that are different for columns 
with the same name. This is particularly useful if you have multiple installations of database schemas and wonder if they match 
(e.g. for test/productive environments); a way to automatically create 'alter table'-scripts is planned.

Aliases
-------

For certain repeating tasks its tedious to write the same command over and over again. Therefore it is possible to store aliases.

    henman@oracle:localhost> alias ls tables

This would alias the `tables` command so that it can be called by simply typing `ls`.

Parameters given to aliases are appended to the original command, so that you can define aliases for commands that need parameters:

    henman@oracle:localhost> alias size select count(*) from
    henman@oracle:localhost> size footab
    execute alias: select count(*) from footab
    ----------+
     count(*) |
    ----------+
            9 |
    ----------+
    1 row in result (first row: 3 msec; total: 3 msec)

Note, that even TAB-completion (in this case: of the tablename) works: the alias
command peeks into the original command that is executed when you type `size`.

All aliases can be shown with `list-aliases`; they are stored, so that they are available on next startup.

Keybindings
-----------

In HenPlus it is possible to bind function keys to commands to be executed (a bit depending on the terminal, but should work on 
usual Unix terminals). It is possible to bind an arbitrary string to a function key; the string is issued whenever the function 
key is pressed. This can be used as a shortcut to excute commands; so instead of pressing help<RETURN> you could bind the same 
command to a function key:

    bind-key-cmd F1 "help\n"

The function keys F1..F12 and Shift-F1..Shift-F12 are available.

Setting Command Bookmarks
-------------------------

Sometimes it would be nice set a 'bookmark' to a command just executed. With the keybinding this is simple to do. Consider, 
you want to set a bookmark with Shift-F2 and execute it with F2. So what we want to do on Shift-F2 is to bind the command we 
just executed to F2. With a little quoting and use of the special variable ${_HENPLUS_LAST_COMMAND} we can do the trick:

    bind-key-cmd Shift-F2 "bind-key-cmd F2 \"$${_HENPLUS_LAST_COMMAND};\\n\"\n"

to bind the last command executed to F2 (Note the double $$ and the \\ to prevent certain elements to be expanded prematurely).
So executing Shift-F2 will now execute the command

    bind-key-cmd F2 "${_HENPLUS_LAST_COMMAND};\n"

that binds F2 to the command that has been just executed. So pressing Shift-F2 sets a bookmark and F2 executes it.

Database independent table dumps
--------------------------------

You can dump out tables in a database independent format. See the online help for `dump-out` and `dump-in`. You even can dump 
only selected values of a certain table; see `dump-conditional` for this.

    henman@oracle:localhost> dump-out mytables.dump.gz student addresses;

... dumps out the tables `student` and `addresses` to the file `mytables.dump.gz`. Note, that the file is gzipped automatically
on-the-fly if the file contains a `.gz`-suffix.

The format that is written must be database independent, thus it is not possible to store them as simple `INSERT INTO..` 
statements, as the different databases have different assumption how some data types should be parsed (esp. dates..) :-( Thus the 
dump format is a canonical text format that resembles the original insert-statement arguments; it is easily
parseable for the human eye and external tools; it is sufficiently simply to create `INSERT INTO`-scripts with simple Shell scripts:

    (tabledump 'student'
       (file-encoding 'UTF-8')
       (dump-version 1 1)
       (henplus-version '0.9.1')
       (database-info 'MySQL - 3.23.47')
       (meta ('name',   'sex',    'student_id')
             ('STRING', 'STRING', 'INTEGER'   ))
       (data ('Megan','F',1)
             ('Joseph','M',2)
             ('Kyle','M',3)
             ('Mac Donald\'s','M',4))
       (rows 4))

Note, not all data types are supported yet: the CLOB and BLOB data type are handled too 
differently even though JDBC should allow access database independently. This will be
included in a future

**And what about XML ?**

One could argue, that XML would be an idea as export format, since it is hype and therefore should be used everywhere :-) ... 
But seriously, I decided against it because it blows up the file size (and database exports tend not to be small) and is not 
very human readable due to the 'noise'. If you still think, that XML would be a good idea, then consider normal use-cases 
of dumps like this (besides dump-in with HenPlus of course):

* Process them with line oriented tools (like `awk`, `sed` .. or your favourite editor)
* Construct insert/update scripts for databases manually.

Both use-cases are addressed with this format: each data record is on a single line and is almost compatible with the typical 
set-syntax of SQL. You can simply construct an insert-Statement with this format with only minimal editing required like:

   insert into student (name, sex, student_id) values ('Joseph','M',2);

Ok, XML would be great for many other tools (as long as humans don't have to deal with it) .. but I don't use them. So if you 
*really* want XML export/import, you are free to do it: just go ahead and write a plugin that does this and let me know for 
integration.

Tree View of connected tables
-----------------------------

Know this? To just find out the datastructure of foreign-key connected tables in your database you have to `describe` manually 
through all tables. This is quite tedious. Thus, HenPlus provides a tree view of your tables. Cyclic references are resolved by 
printing the recursive entity in parenthesis.  See the example for some bugtracker database:

    henman@oracle:localhost> tree-view BT_TRACKERUSER
    
    BT_TRACKERUSER
    |-- BT_BUGHISTORY
    |   |-- BT_BUG
    |   |   `-- (BT_BUGHISTORY)
    |   `-- BT_BUGCOMMENTATTACHMENT
    `-- BT_USERPERMISSION
    265 msec

FAQ
---

This will eventually be the FAQ.

**Q** When I am connected to a postgreSQL database, sometimes theconnection seems to stop working. No select works.

**A** This is primarily not a problem with HenPlus, but in general with Postgres when it encounters an error in some SQL-statement.
Since everything in Postgres is done within a transaction, a transaction is regarded invalid after some error; any subsequent 
commands are ignored, until you finish the transaction with 'commit' or 'rollback'. This might be annoying if you run SQL-scripts 
-- if there is only one error, all subsequent commands are ignored. One solution might be to switch on autocommit (HenPlus command: 
`set-session-property auto-commit on`).

**Q** Is it possible to have henplus use the vi-editing mode rather than the default emacs like editing mode?

**A** Create/edit the `~/.inputrc` configuration file to influence the behaviour of readline. Add a line `set editing-mode vi`
To this file. Then set the environment-variable INPUTRC to point to this file: `export INPUTRC=~/.inputrc` (thanks to Scott Plante)
