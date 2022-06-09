/*
 * Copyright 2015 - 2016 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inet.gradle.setup.dmg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.control.ConfigurationException;
import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;
import com.inet.gradle.setup.abstracts.Application;
import com.inet.gradle.setup.abstracts.DocumentType;
import com.oracle.appbundler.AppBundlerTask;
import com.oracle.appbundler.Architecture;
import com.oracle.appbundler.Argument;
import com.oracle.appbundler.BundleDocument;
import com.oracle.appbundler.Option;
import com.oracle.appbundler.Runtime;

/**
 * Abstract implementation for creating the resulting app bundler image
 *
 * @author gamma
 *
 * @param <T> the Task
 * @param <S> the SetupBuilder
 */
public abstract class AbstractOSXApplicationBuilder<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T, S> {

    private S              setup;

    private AppBundlerTask appBundler;

    /**
     * Setup this builder.
     *
     * @param task - original task
     * @param setup - original setup
     * @param fileResolver - original fileResolver
     */
    protected AbstractOSXApplicationBuilder( T task, S setup, FileResolver fileResolver ) {
        super( task, fileResolver );
        this.setup = setup;
        appBundler = new AppBundlerTask();
    }

    /**
     * Prepare the basic settings of the application
     *
     * @param application to use
     * @param isJNLPBuild indicate that this will be a JNLP application
     * @throws Exception on errors
     */
    protected void prepareApplication( Application application, boolean isJNLPBuild ) throws Exception {

        String appName = application.getDisplayName();

        task.getProject().getLogger().lifecycle( "\tBuildDir now: " + buildDir );
        appBundler.setOutputDirectory( buildDir );
        appBundler.setName( appName );
        appBundler.setDisplayName( appName );

        String version = task.getVersion();
        appBundler.setVersion( version );
        int idx = version.indexOf( '.' );
        if( idx >= 0 ) {
            idx = version.indexOf( '.', idx + 1 );
            if( idx >= 0 ) {
                version = version.substring( 0, idx );
            }
        }

        // Get the working directory and patch the main jar with it.
        String mainJar = application.getMainJar();
        if( application.getWorkDir() != null ) {
            appBundler.setWorkingDirectory( new File( new File( "$APP_ROOT/Contents/Java" ), application.getWorkDir() ).toString() );
            mainJar = new File( new File( application.getWorkDir() ), mainJar ).toString();
        }

        appBundler.setShortVersion( version );
        appBundler.setExecutableName( application.getExecutable() );

        String identifier = setup.getAppIdentifier();
        if( appName != setup.getApplication() ) {
            identifier += "." + appName.replaceAll( "^A-Za-z0-9]", "" );
        }

        appBundler.setIdentifier( identifier );

        if ( !isJNLPBuild ) {
            // Main Class and Jar File are required.
            if ( application.getMainClass() == null ) {
                throw new ConfigurationException( "A main class is required for the application. You have to configure at least the following:\n\n\tsetupBuilder {\n\t\t[..]\n\t\tmainClass = 'your.org.main.class'\n\t}\n\n" );
            }
            appBundler.setMainClassName( application.getMainClass() );

            // Check for mainJar to not be null
            if ( mainJar == null ) {
                throw new ConfigurationException( "A main jar file is required for the application. You have to configure at least the following:\n\n\tsetupBuilder {\n\t\t[..]\n\t\tmainJar = '/path/to/yourMain.jar'\n\t}\n\n" );
            }
            appBundler.setJarLauncherName( mainJar );

            application.getJavaVMArguments().forEach( arg -> {
                Option argument = new Option();
                argument.setValue( arg );
                appBundler.addConfiguredOption( argument );
            });

            Arrays.asList( application.getStartArguments().split( " " ) ).forEach( arg -> {

                if ( arg.isEmpty() ) {
                    return;
                }

                Argument argument = new Argument();
                argument.setValue( arg );
                appBundler.addConfiguredArgument( argument );
            });
        }

        appBundler.setIgnorePSN( true ); // Ignore this argument.
        appBundler.setCopyright( setup.getCopyright() );
        appBundler.setIcon( getApplicationIcon() );

        // Add all native libraries
        if( task instanceof Dmg ) {
            ((Dmg)task).getArchitecture().forEach( arch -> appBundler.addConfiguredArch( Architecture.from( arch ) ) );
            ((Dmg)task).getNativeLibraries().forEach( set -> appBundler.addConfiguredLibraryPath( set ) );
        }
    }

    /**
     * Execute the appbundler, create the final .app file
     */
    protected void finishApplication() {
        bundleJre();

        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
        appBundler.setProject( antProject );
        appBundler.execute();
    }

    /**
     * Set the document types from the list
     *
     * @param list of document types
     * @throws IOException on complications with the icon
     */
    protected void setDocumentTypes( List<DocumentType> list ) throws IOException {
        // add file extensions
        for( DocumentType doc : list ) {
            BundleDocument bundle = new BundleDocument();
            bundle.setExtensions( String.join( ",", doc.getFileExtension() ) );
            bundle.setName( doc.getName() );
            bundle.setRole( doc.getRole() ); // Viewer or Editor
            bundle.setIcon( getApplicationIcon().toString() );
            appBundler.addConfiguredBundleDocument( bundle );
        }
    }

    /**
     * Adds a scheme to the application. That is: a protocol that will trigger this application
     * @param scheme a scheme to add
     */
    protected void addScheme( String scheme ) {
        if ( scheme == null || scheme.isEmpty() ) {
            return;
        }

        Argument schemeArgument = new Argument();
        schemeArgument.setValue( scheme );
        appBundler.addConfiguredScheme( schemeArgument  );
    }

    /**
     * Bundle the Java VM if set.
     */
    protected void bundleJre() {
        Object jre = setup.getBundleJre();
        if( jre == null ) {
            return;
        }
        File jreDir;
        try {
            jreDir = task.getProject().file( jre );
        } catch( Exception e ) {
            jreDir = null;
        }
        if( jreDir == null || !jreDir.isDirectory() ) {
            ArrayList<String> command = new ArrayList<>();
            command.add( "/usr/libexec/java_home" );
            command.add( "-v" );
            command.add( jre.toString() );
            command.add( "-F" );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exec( command, null, baos );
            jreDir = new File( baos.toString().trim() );
            if( !jreDir.isDirectory() ) {
                throw new GradleException( "bundleJre version " + jre
                                + " can not be found in: " + jreDir );
            }
        }
        task.getProject().getLogger().lifecycle( "\tbundle JRE: " + jreDir );

        Runtime runtime = new Runtime( jreDir );
        if ( task instanceof Dmg ) {
            List<String> include = ((Dmg)task).getJreIncludes();
            List<String> exclude = ((Dmg)task).getJreExcludes();

            runtime.appendIncludes( include.toArray( new String[include.size()] ) );
            runtime.appendExcludes( exclude.toArray( new String[exclude.size()] ) );
        } else {
            runtime.appendIncludes( new String[] { "jre/bin/java" } );
        }

        appBundler.addConfiguredRuntime( runtime );
    }

    /**
     * Returns the icns application icon file
     *
     * @return Icon file
     * @throws IOException when there are errors while getting the file
     */
    protected File getApplicationIcon() throws IOException {
        File icons = setup.getIconForType( buildDir, "icns" );
        if( icons == null ) {
            throw new ConfigurationException( "You have to specify a valid icon file.\n\n\tPlease set the parameter 'icons' of the setupBuilder configuration to an existing '*.icns' file.\n\n" );
        }
        return icons;
    }

    /**
     * Modify a plist file
     *
     * @param plist file to modify
     * @param property property to set
     * @param value of property
     */
    protected void setPlistProperty( File plist, String property, String value ) {

        // Set Property in plist file
        // /usr/libexec/PlistBuddy -c "Set PreferenceSpecifiers:19:Titles:0 $buildDate" "$BUNDLE/Root.plist"
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/libexec/PlistBuddy" );
        command.add( "-c" );
        command.add( "Set " + property + " " + value );
        command.add( plist.getAbsolutePath() );
        exec( command );
    }

    /**
     * Add an entry to a plist file
     *
     * @param plist file to modify
     * @param property property to set
     * @param type of property
     * @param value of property
     */
    protected void addPlistProperty( File plist, String property, String type, String value ) {

        // Set Property in plist file
        // /usr/libexec/PlistBuddy -c "Add PreferenceSpecifiers:19:Titles:0 $buildDate" "$BUNDLE/Root.plist"
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/libexec/PlistBuddy" );
        command.add( "-c" );
        command.add( "Add " + property + " " + type + " " + value );
        command.add( plist.getAbsolutePath() );
        exec( command );
    }

    /**
     * Add an entry to a plist file
     *
     * @param plist file to modify
     * @param property property to set
     */
    protected void deletePlistProperty( File plist, String property ) {

        // Set Property in plist file
        // /usr/libexec/PlistBuddy -c "Add PreferenceSpecifiers:19:Titles:0 $buildDate" "$BUNDLE/Root.plist"
        exec( "/usr/libexec/PlistBuddy", "-c", "Delete " + property, plist.getAbsolutePath() );
    }

    /**
     * Copy the files defined in the gradle script into their final destination
     *
     * @param application the application
     * @throws IOException on errors
     */
    protected void copyBundleFiles( Application application ) throws IOException {
        File destination = new File( buildDir, application.getDisplayName() + ".app" );
        getTask().copyTo( new File( destination, "Contents/Java" ) );

        setApplicationFilePermissions( destination );
    }

    /**
     * Set File permissions to the resulting application
     *
     * @param destination of the files to manipulate
     * @throws IOException on errors
     */
    protected void setApplicationFilePermissions( File destination ) throws IOException {

        // Set Read on all files and folders
        ArrayList<String> command = new ArrayList<>();
        command.add( "chmod" );
        command.add( "-R" );
        command.add( "a+r" );
        command.add( destination.getAbsolutePath() );
        exec( command );

        // Set execute on all folders.
        command = new ArrayList<>();
        command.add( "find" );
        command.add( destination.getAbsolutePath() );

        if( destination.isDirectory() ) {
            command.add( "-type" );
            command.add( "d" );
        }

        command.add( "-exec" );
        command.add( "chmod" );
        command.add( "a+x" );
        command.add( "{}" );
        command.add( ";" );
        exec( command );
    }

    /**
     * Return the current AppBundler.
     *
     * @return appBundler
     */
    protected AppBundlerTask getAppBundler() {
        return appBundler;
    }

    protected S getSetupBuilder() {
        return setup;
    }
}
