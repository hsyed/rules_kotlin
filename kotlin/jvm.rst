JVM Rules
=========

Common Attributes
~~~~~~~~~~~~~~~~~
.. role:: param(kbd)
.. role:: type(emphasis)
.. role:: value(code)

+------------------------------------------------------+-----------------------+---------------------------------------+
| **Name**                                             | **Type**              | **Default value**                     |
+------------------------------------------------------+-----------------------+---------------------------------------+
| :param:`srcs`                                        | :type:`list of File`  | []                                    |
+------------------------------------------------------+-----------------------+---------------------------------------+
| The list of source files that are processed to create the target.                                                    |
|                                                                                                                      |
| The following file types are supported:                                                                              |
| **kt**                                                                                                               |
|   A single Kotlin source file must be provided.                                                                      |
| **java**                                                                                                             |
|   Java source files. Mixed-Mode compilation is supported which enables Kotlin and Java to co-reference in a single   |
|   compilation unit.                                                                                                  |
| **srcjar**                                                                                                           |
|   Files with the extension `.srcjar` enable code generation patterns. The sources contained in this jar are expanded |
|   during compilation and added to the source list. Such sources are also included in the `-sources.jar` output       |
+------------------------------------------------------+-----------------------+---------------------------------------+
