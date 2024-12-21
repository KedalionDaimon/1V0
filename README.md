# 1V0
1V0  (Pronounced "Ivo".)

Presented is a new programming language/virtual machine for ATTiny85, Arduino UNO/MEGA2560/DUE, ESP8266, Java MIDP 1.0/2.0 (CLDC 1.1) enabled phones, of assembler-like nature and operating on floating point (or fixed point) numbers as its exclusive data type, and with implementations in C and Java for the command line. The aim is a most portable "small mainframe in early 1940s to 1960s style" that can practically run on nearly any ever so feeble device. It shall fill the niche where BASIC interpreters are "too big" or not comfortable enough to use. Where practicable, it runs from (at least emulated) EEPROM.

YouTube:  https://youtu.be/dZ4dWOTq1ac

GitHub:  https://github.com/KedalionDaimon/1V0

The Java files without date are the midlets, whereby IVO.jar is the MIDP2.0/CLDC1.1 midlet and IVOnano.jar is the MIDP1.0 midlet which should run on the most ancient Java-enabled mobiles.

Regarding how to use it, check the PDF documentation and the Youtube video.

"To start as quickly as possible", run on your preferred desktop OS (should be fine with Linux, Windows, Mac, and various BSDs):

java -jar IVO_Java.jar

If you prefer to compile in C, practically any C compiler should work, but e.g. in GCC just do:

gcc -o 1V0 1V0_20200709.c  -lm -Warray-bounds -Wstrict-aliasing

The "reference implementation" is the C version.

2024-12-14:
.
.
.
MANUAL
.
.
.
For all systems of December 2024, refer to 1V0TzIV_ZXSpectrum.txt - it was started for the ZX Spectrum 48K, but applies to all systems.

.
.
.
ZX Spectrum: ivo_tz_4.tap
.
.
.

The so far most portable variant "1V0 Tzetanka IV" has been ported to the ZX Spectrum - see the files 1V0TzIV220zx_20241213a.c and ivo_tz_4.tap, compiled with the following command using z88dk:

zcc +zx -clib=new -create-app 1V0TzIV220zx_20241213a.c -o ivo -create-app

.
.
.
CP/M: IVOCPM.COM, IVOPX8.COM
.
.
.

For CP/M, to compile, use:

ack -mcpm -o IVOCPM.COM 1V0TzIV220zx_20241213a.c

and for the Epson PX-8 "Geneva", which can only operate a subset of the above due to space constraints, use:

ack -mcpm -o IVOPX8.COM 1V0TzIV220zx_20241213a_mini.c

(Might need to explicitly use Ctrl-M,Ctrl-J for "Enter".)

Missing from the PX8 version are the following instructions: CORS, SORT, TURN, POXY, SQRT, SQUA, CBRT, CUBE, AMNT, MSTD, SIND, COSD, TAND

(How to transfer via RS232 to the Epson PX-8 Geneva from Linux:

Set up the port on /dev/ttyUSB0, usually the first serial port on Linux, alternatively /dev/ttyACM0:

sudo stty -F /dev/ttyUSB0 300 cs8 -parenb -cstopb -echo -ixon raw

possibly also:

picocom --baud 300 --parity n --databits 8 --stopbits 1 --flow n --receive-cmd "rz --vv" --send-cmd "sz" --emap delbs --imap del
bs,crcrlf /dev/ttyUSB0

Ctrl-A,Ctrl-Q

On the Epson PX8: for receipt, use term, and record (while displaying):

dd if=IVOPX8.COM bs=1 | pv -L 20 | dd of=/dev/ttyUSB0 status=progress

where pv slows down the transfer rate to 20 characters per second.)

Possible alternative compilation:

zcc +cpm -clib=new -create-app 1V0TzIV220zx_20241213a.c -o ivocpm -create-app

creates IVOCPM.COM

.
.
.
DOS
.
.
.

Compile with Turbo C, after replacing uint16_t by unsigned int - see IVTZ4DOS.C .

Use the Compiler Option Memory Model Tiny.

Use Exe2Bin from FreeDOS to turn EXE into COM.

Transfer to Atari Portfolio using xterm.com with picocom using sx (XMODEM) file transfer protocol.

.
.
.
Commodore 64: ivotz4.d64
.
.
.

cl65 -O -t c64 1V0TzIV220zx_20241213a.c -o IVOTZ4.PRG

Make sure the result is called IVOTZ4.PRG and put it onto a disk image - note, cc1541 is installed separately from VICE:

cc1541 -w IVOTZ4.PRG -v ivotz4.d64

Works then also in Linux with wine CCS64.exe and setting that as startup file

you can simply load"$",8 to see directory or load "matrix.prg",8 in Vice...

wine x64 -8 /home/archon/Desktop/exp/1V0/1V0TzIV20230416/ivotz4.d64

(Native Linux ends in utter failure:
cd /usr/lib/vice ; x64 -8 /home/archon/Desktop/exp/1V0/1V0TzIV20230416/ivotz4.d64
"?DEVICE NOT PRESENT ERROR" )

LOAD"*.PRG",8
RUN

(The name on the disk is total letter salad...)

Runs it in VICE, too.

LOAD"$",8
LIST
should give you a directory listing...?

Runs it immediately:

wine /home/archon/Documents/var/C64/CCS64.exe ivotz4.d64

.
.
.
Apple II, Apple IIe etc: ivtz4aii.dsk
.
.
.

This is a ProDOS disk, 1V0 Tz IV being based in the HELLO file (equivalent to AUTOEXEC.BAT in DOS), and even runs in:

https://paleotronic.com/2021/07/28/cyaniide-web-based-apple-iie-emulator/

From the cc65 wiki:

https://github.com/cc65/wiki/wiki/Apple-II-3.-Making-an-Apple-II-disk-for-an-emulator

Get a ProDOS disk from https://prodos8.com/.

Get a release of the Apple Commander: https://github.com/AppleCommander/AppleCommander/releases

Launch it, e.g.:

java -jar AppleCommander-linux-x86_64-1.9.0.jar

... and open the disk:

"Open the downloaded ProDOS disk and delete all the files except for the file named PRODOS
Save this disk to a new name, template.dsk and keep this safe - it will be reused all the time."

Done, saved as "empty_apple2_prodos.dsk". Copied to "iz4appl2.dsk".

Then do:

cl65 --print-target-path

which gives you a path, e.g.:

/usr/share/cc65/target

... which is then used to determine the location of

/usr/share/cc65/target/apple2/util/loader.system.

Then run:

cl65 -O -t apple2 1V0TzIV220zx_20241213a_mini.c -o hello.apple2

(It can compile only the mini version, i.e. missing: CORS, SORT, TURN, POXY, SQRT, SQUA, CBRT, CUBE, AMNT, MSTD, SIND, COSD, TAND.)

java -jar AppleCommander-ac-1.9.0.jar -p ivtz4aii.dsk hello.system sys < /usr/share/cc65/target/apple2/util/loader.system

java -jar AppleCommander-ac-1.9.0.jar -as ivtz4aii.dsk hello bin < hello.apple2 

(note, these are CLI applets.)

Then, to emulate, you might e.g. get this (or use the online emulator mentioned above):

https://github.com/AppleWin/AppleWin/releases

... and run it:

wine applewin

Select the disk now created, it will just boot straight into 1V0 Tz IV.

F6 switches fullscreen vs normal; F8 lets you select another monitor.

.
.
.
Linux (Ubuntu 22.04, statically compiled): ivotz4
.
.
.

Linux (Ubuntu 22.04), permitting the creation of a static executable:

gcc -static -o ivotz4 1V0TzIV220zx_20241213a.c

.
.
.
Windows 11: ivtz4win.exe 
.
.
.

Windows, using Fabrice Bellard's wonderful TCC compiler, but not permitting static compilation:

tcc -m32 -o ivtz4win.exe 1V0TzIV220zx_20241213a.c

.
.
.
macOS 15.2 Sequoia: ivtz4mac
.
.
.

macOS, not permitting static compilaton:

gcc -o ivtz4mac 1V0TzIV220zx_20241213a.c

.
.
.
Haiku OS: ivtz4hai (32bit, dynamically linked)
.
.
.

Haiku OS (Haiku Beta 5), not permitting static compilation (compiled 32 bit on Asus Eee PC 701 4):

gcc -o ivtz4hai 1V0TzIV220zx_20241213a.c

.
.
.
iOS using iSH (the "i Shell"): ivtz4ish
.
.
.

iOS, permitting static compilation within the iSH environment:

gcc -static -o ivtz4ish 1V0TzIV220zx_20241213a.c

(This is really an i386 Linux environment, and Ubuntu 22.04 runs this just fine, too.)

.
.
.
Android using Termux (aarch64): ivtz4tmx (statically compiled)
.
.
.

Android using Termux has been rather involved - compiling a static executable under Termux is allegedly "out of scope" for the Termux developers, at least as of recent online discussions, but can be done as follows:

gcc -o ivtz4tmx -L/data/data/com.termux/files/usr/opt/ndk-multilib/arm-linux-androideabi/lib -static 1V0TzIV220zx_20241213a.c

wget https://gist.githubusercontent.com/hamjin/1884bdc41bd33ff6a19ab1c2802c8750/raw/d4917b80181aeee58ee29f6a44dce4f097b9354b/align_fix.py

chmod +x align_fix.py

./align_fix.py ivtz4tmx

./ivtz4tmx

will finally work and give you a static executable running under Termux

(Otherwise Termux behaves as follows:

gcc -o ivtz4tmx -static 1V0TzIV220zx_20241213a.c                                ld.lld: error: unable to find library -lc gcc: error: linker command failed with exit code 1 (use -v to see invocation)

gcc -o ivtz4tmx -L/data/data/com.termux/files/usr/opt/ndk-multilib/aarch64-linux-android/lib -static 1V0TzIV220zx_20241213a.c
~ $ ./ivtz4tmx error: "./ivtz4tmx": executable's TLS segment is underaligned: alignment is 8 (skew 0), needs to be at least 64 for ARM64 Bionic
Aborted)

Nino Ivanov
