/*--------------------------------------------------------------------------
|   rxtx-rebundled LibLoader is a helper class for rxtx library.
|   Copyright 2012 by Alexander Christian alex@root1.de
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Library General Public License for more details.
|
|   You should have received a copy of the GNU Library General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
--------------------------------------------------------------------------*/
package de.root1.rxtxrebundled;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper Class used by rxtx implementation to load native libs without having 
 * java.library.path property specified. 
 * Native libs will be extracted and loaded automatically, if possible
 *
 * @author achr
 * @author sitec systems GmbH
 */
public class LibLoader {

    private static final String LIBLOADER_VERSION_NAME = "rxtx-rebundled-2.1-7r3";
    private static final String HF_ABI_SUPPORT = "Tag_ABI_VFP_args: VFP registers";

    /**
     * <code>Map&lt;Name of lib i.e. 'rxtxSerial', path to extracted lib&gt;</code><br>
     */
    private static final Map<String, String> extractedLibs = new HashMap<String, String>();

    static {

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        logStdOut("os.name='"+osName+"'");
        logStdOut("os.arch='"+osArch+"'");

        // check for linux platform ..
        if (osName.toLowerCase().contains("linux")) {
            // check for architecture arm
            if (osArch.toLowerCase().contains("arm")) {
                try {
                    // check for hard float or soft float abi
                    if (isHardFloatAbi()) {
                        logStdOut("Float ABI=Hard Float (armhf)");
                        // armhf libs from Debian project (https://packages.debian.org/wheezy/librxtx-java)
                        extractedLibs.put("rxtxSerial", extractLib("/jni/Linux/armhf-unknown-linux-gnu/", "librxtxSerial.so"));
                    } else {
                        logStdOut("Float ABI=Soft Float (armel)");
                        // armel libs from Debian project (https://packages.debian.org/wheezy/librxtx-java)
                        extractedLibs.put("rxtxSerial", extractLib("/jni/Linux/armel-unknown-linux-gnu/", "librxtxSerial.so"));
                    }
                } catch (final IOException ex) {
                    logStdErr("The check for arm float ABI has failed: " + ex.getMessage());
                    logExceptionToStdErr(ex);
                }
            }
            // check for architecture 64bit
            else if (osArch.toLowerCase().contains("amd64") || osArch.toLowerCase().contains("x86_64")) {
                extractedLibs.put("rxtxSerial", extractLib("/jni/Linux/x86_64-unknown-linux-gnu/", "librxtxSerial.so"));
            }
            // else 32bit
            else {
                extractedLibs.put("rxtxSerial", extractLib("/jni/Linux/i686-unknown-linux-gnu/", "librxtxSerial.so"));
                extractedLibs.put("rxtxParallel", extractLib("/jni/Linux/i686-unknown-linux-gnu/", "librxtxParallel.so"));
            }

        }
        // check for windows platform
        else if (osName.toLowerCase().contains("windows")) {

            // check for architecture 64bit
            if (osArch.toLowerCase().contains("amd64") || osArch.toLowerCase().contains("x86_64")) {
                // 64bit libs from http://www.cloudhopper.com/opensource/rxtx/
                extractedLibs.put("rxtxSerial", extractLib("/jni/Windows/x64-VisualCpp2008/", "rxtxSerial.dll"));
                extractedLibs.put("rxtxParallel", extractLib("/jni/Windows/x64-VisualCpp2008/", "rxtxSerial.dll"));
            }
            // else 32bit
            else {
                extractedLibs.put("rxtxSerial", extractLib("/jni/Windows/i368-mingw32/", "rxtxSerial.dll"));
                extractedLibs.put("rxtxParallel", extractLib("/jni/Windows/i368-mingw32/", "rxtxParallel.dll"));
            }

        }
        // check for os x platform
        else if (osName.toLowerCase().contains("os x")) {

               //If this is OS x then we'll need to create the lock directory that's required by rxtx
            /*try
            {
                File lockDir = new File("/var/lock");
                FileUtils.forceMkdir(lockDir);
                lockDir.setReadOnly();
                lockDir.setWritable(true);
                lockDir.setExecutable(true);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }*/

            // no arch available or required?!
            extractedLibs.put("rxtxSerial", extractLib("/jni/Mac_OS_X/", "librxtxSerial.jnilib"));

        }
        // check for solaris platform
        else if (osName.toLowerCase().contains("solaris")) {

            // check for architecture 64bit
            if (osArch.toLowerCase().contains("amd64") || osArch.toLowerCase().contains("x86_64")) {
                extractedLibs.put("rxtxSerial", extractLib("/jni/Solaris/sparc-solaris/sparc64-sun-solaris2.8/", "librxtxSerial.so"));
            }
            // else 32bit
            else {
                extractedLibs.put("rxtxSerial", extractLib("/jni/Solaris/sparc-solaris/sparc32-sun-solaris2.8/", "librxtxSerial.so"));
            }

        }
        // other platforms are currently not supported ...
        else {
            logStdOut("Sorry, platform '"+osName+"' currently not supported by LibLoader. Please use -Djava.library.path=<insert path to native libs here> as JVM parameter...");
        }

        logStdOut("Map: "+extractedLibs);

    }

    /**
     * Checks the platform for hard float abi support.
     * @return <code>true<code> - The hard float abi is supported / <code>false</code>
     *         - Only soft float abi is supported
     * @throws java.io.IOException The reading of abi from ELF header has failed
     */
    private static boolean isHardFloatAbi() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder("readelf", "-A", "/proc/self/exe");
        final Process p = pb.start();
        try {
            p.waitFor();
        } catch (final InterruptedException ex) {
            logStdErr("The ABI request was interrupted");
        }

        if(p.exitValue() != 0) {
            throw new IOException("The ABI request has failed");
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            boolean result = false;

            while((line = br.readLine()) != null) {
                if(line.contains(HF_ABI_SUPPORT)) {
                    result = true;
                    break;
                }
            }

            return result;
        } finally {
            if(br != null) {
                br.close();
            }
        }
    }

    /**
     * Extracts the specified library from resource dir to a temp folder 
     * and returns the path to the extracted file
     *
     * @param folder resource folder of the lib to extract
     * @param libFileName name of the lib file in resource folder
     * @return complete path to the extracted lib
     */
    private static String extractLib(String folder, String libFileName) {
        int written = 0;

        String libSource = folder + libFileName;

        InputStream resourceAsStream = null;
        try {
            resourceAsStream = LibLoader.class.getResourceAsStream(libSource);

            if (resourceAsStream != null) {

                File tempFolder = new File(System.getProperty("java.io.tmpdir"));

                // Workaround for getting a temp-folder
                File tmpFile = File.createTempFile(LIBLOADER_VERSION_NAME, "", tempFolder);
                String rxtxTmpName = tmpFile.getName();
                tmpFile.delete();

                File rxtxTmp = new File(tempFolder, rxtxTmpName);
                rxtxTmp.mkdirs();
                rxtxTmp.deleteOnExit();

                File libFile = File.createTempFile("tmp_", "_" + libFileName, rxtxTmp);
                libFile.deleteOnExit();

                FileOutputStream fos = new FileOutputStream(libFile);
                int read = 0;
                byte[] data = new byte[512];
                while ((read = resourceAsStream.read(data)) != -1) {
                    fos.write(data, 0, read);
                    written += read;
                }
                fos.close();
                logStdOut("Extracting " + libSource + " to "+libFile.getAbsolutePath()+" *done*. written bytes: " + written);
                return libFile.getAbsolutePath();
            } else {
                logStdOut("Could not find " + libSource + " in resources ...");
            }
        } catch (Exception ex) {
            logStdErr("Error extracting " + libSource  + " to temp... written bytes: " + written);
            logExceptionToStdErr(ex);
        } finally {
            if (resourceAsStream!=null) {
                try {
                    resourceAsStream.close();
                } catch (IOException ex) {
                }
            }
        }
        return null;
    }

    private static void logStdOut(String msg) {
        String property = System.getProperty("rxtx.rebundled.debug", "false");
        boolean log = Boolean.parseBoolean(property);
        if (log) {
            System.out.println(msg);
        }
    }

    private static void logStdErr(String msg) {
        String property = System.getProperty("rxtx.rebundled.suppress_error", "false");
        boolean suppress = Boolean.parseBoolean(property);
        if (!suppress) {
            System.err.println(msg);
        }
    }

    private static void logExceptionToStdErr(Exception ex) {
        String property = System.getProperty("rxtx.rebundled.suppress_error", "false");
        boolean suppress = Boolean.parseBoolean(property);
        if (!suppress) {
            ex.printStackTrace();
        }
    }

    public static void loadLibrary(String name) {

        logStdOut("Trying to load '"+name+"' ...");

        String lib = extractedLibs.get(name);

        try
        {
            // check if it's loadable via LibLoader mechanism
            if (lib!=null) {
                File f = new File(lib);
                logStdOut("...Loading via LibLoader mechanism: "+f.getAbsolutePath());
                System.load(f.getAbsolutePath());
            }
            // otherwise load it the old way ...
            else {
                logStdOut("...Loading via System.loadLibrary() call");
                System.loadLibrary(name);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }


        logStdOut("...*done*");
    }

}