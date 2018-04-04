Numerous people have contributed to make this software what it is
today. The Nxt-developers before Burst was forked off from the Nxt
code base, the initial - yet anonymous - creator of burstcoin who did
carve out PoS and instantiate PoC and also many small contributors who
helped out with small fixes, typos, translations etc. In case we
forgot to mention someone, please do not hesitate to bring this to our
attention. Past contributions that have been removed from the code
base are not mentioned, however. In alphabetical order:

@4nt1g0
* helping with JOOQ migration

@ac0v
* initial replacement of the H2 wallet against mariaDB in the CG-lineage of the wallet
* introduction of FirebirdDB to the list of supported DB backends, bugfixing, debugging
* streamlining helper scripts (invocation, compilation)
* work on macOS port, testing and release management
* JOOQ migration and many more things

@Brabantian
* introduction of JUnit and Code Coverage reporting
* lots of unit tests and refactoring work to make the code testable

@BraindeadOne
* providing a DB abstraction layer to allow for multiple DB backends
* implementing a load/dump mechanism for faster sync (called Quicksync)
* added SQL metrics to the code to be able to spot DB performance problems
* CPU core limit patch, so the wallet can be assigned only part of your resources

@chrulri improved wallet behavior when running under https

@daWallet did many bugfixes and stabilization improvements

@dcct
* support for parallel blockchain downloads
* various bugfixes and lib updates

@de-luxe took care of the releases between 1.2.5 and 1.2.9

@fusecavator
* initial OpenCL code (GPU acceleration support)
* critical fixes to the wallet spam-attack vulnerability

@LithMage provided fixes and enhancements to the UI

@Quibus
* DownloadCache
* PoC2 implementation
* improved peer handling
* lots of bugfixes

@rico666
* moved the wallet from NRS/Nxt to BRS/Burst namespace
* improvements and fixes to the documentation - revival of javadoc references
* general code refactoring and styleguide unification (Google JAVA Styleguide)
* removed obsolete/unused code - tens of thousands of LOCs
* fixes and enhancements to the UI, config streamlining also JS updates

Other contributors

Accepted pull requests improving the wallet quality in several areas
were made by @ChrisMaroglou, @DarkS0il, @Doncode, @HeosSacer,
@InjectedPie, @jake-b, @llybin, @naiduv, @umbrellacorp03,
@velmyshanovnyi
