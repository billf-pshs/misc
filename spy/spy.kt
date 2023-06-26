//
// A simple hack proxy server that allows lopgging.  See usage()
//
// Bill foote, 6/23/23
//

import java.net.SocketException
import java.net.ServerSocket
import java.net.Socket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Inet4Address;


import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.io.File;

var saveLogs = false;

fun main(argsIn:  Array<String>) {
    val args = mutableListOf<String>();
    for (arg in argsIn) {
	if (arg == "-log") {
	    saveLogs = true;
	} else {
	    args.add(arg);
	}
    }

    if (args.size == 0  || args.size % 3 != 0) {
	usage();
    }
    var i : Int = 0;
    while (i < args.size) {
	Spy(args[i++].toInt(), args[i++], args[i++].toInt()).start();
    }
}

fun usage() {
    print("Usage:  spy [-log] (inPort outAddress outPort)*\n");
    System.exit(1);
}

val lock = ReentrantLock();
var nextLogVal : Int = 1000;

fun getNextLog() : Int {
    lock.withLock {
	return nextLogVal++;
    }
}

public val localInetAddress = getAddress()

private fun getAddress() : InetAddress {
    for (ne in NetworkInterface.getNetworkInterfaces()) {
        for (ie in ne.getInetAddresses()) {
            if (!ie.isLoopbackAddress() && ie is Inet4Address) {
                return ie;
            }
        }
    }
    for (ne in NetworkInterface.getNetworkInterfaces()) {
        for (ie in ne.getInetAddresses()) {
            if (!ie.isLoopbackAddress()) {
                return ie;
            }
        }
    }
    return InetAddress.getLocalHost()
}


class Spy(val inPort: Int, val outAddress: String, val outPort: Int)  {

    public fun start() {
        Thread({ acceptConnections(); }).start();
    }

    private fun acceptConnections() {
        print("accepting connections on ${localInetAddress.hostAddress} " +
		"port $inPort\n");
	val ss = ServerSocket(inPort);
	while(true) {
	    val s = ss.accept();
	    proxyConnection(s);
	}
    }

    private fun proxyConnection(src : Socket) {
	val dest = Socket(outAddress, outPort);
	Thread({
	    copyStream("Outgoing", 
		    src.getInputStream(), dest.getOutputStream());
	}).start();
	Thread({
	    copyStream("Incoming", 
		    dest.getInputStream(), src.getOutputStream());
	}).start();
    }

    private fun copyStream(kind: String, input: InputStream, 
	    output: OutputStream) {
	val buf = ByteArray(65536);
	try {
	    while (true) {
		val n = input.read(buf);
		if (n == -1) {
		    break;
		}
		val log = getNextLog();
		val content = buf.sliceArray(0..(n-1));
		if (saveLogs) {
		    File("log.$log.$kind.$inPort").writeBytes(content);
		}
		output.write(content);
		output.flush();
	    }
	} catch (e : Exception) {
	}
	try {
	    output.close();
	} catch (e : Exception) {
	}
    }
}

