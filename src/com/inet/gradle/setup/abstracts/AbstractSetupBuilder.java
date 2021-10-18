/*
 * Copyright 2015 - 2021 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inet.gradle.setup.abstracts;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectories;

import com.inet.gradle.setup.image.ImageFactory;

import groovy.lang.Closure;

/**
 * The Gradle extension for all setup tasks.
 * @author Volker Berlin
 */
public class AbstractSetupBuilder implements SetupSources {

    protected final Project        project;

    private final CopySpecInternal rootSpec;

    private Object                 destinationDir;

    private String                 vendor;

    private String                 application;

    private String                 version;

    private String                 appIdentifier;

    private String                 archiveName;

    private Object                 icons;

    private Object                 bundleJre;

    private String                 bundleJreTarget;

    private String                 mainClass;

    private String                 mainJar;

    private boolean                failOnEmptyFrom = true;

    private String                 description;

    private String                 copyright;

    /**
     * Create a new instance.
     * @param project current project
     */
    public AbstractSetupBuilder( Project project ) {
        this.project = project;
        this.rootSpec = (CopySpecInternal)project.copySpec( (Closure<CopySpec>)null );

        //init default location
        setDestinationDir( new File( "distributions" ) );
        project.getLogger().lifecycle( "setting default Destination to: " + getDestinationDir() );
    }

    @Internal
    @Override
    public CopySpecInternal getRootSpec() {
        return rootSpec;
    }

    /**
     * Get the current project.
     *
     * @return the project
     */
    @Internal
    public Project getProject() {
        return project;
    }

    /**
     * Override to remove the annotation OutputDirectories in the Copy task. {@inheritDoc}
     */
    @OutputDirectories
    public File getDestinationDir() {
        return new File( project.getBuildDir(), String.valueOf( destinationDir ) );
    }

    /**
     * All the inputs
     * @return FileTree
     */
    @InputFiles
    @Override
    public FileTree getSource() {
        FileTree allSource = rootSpec.buildRootResolver().getAllSource();
        if ( allSource == null ) {
            throw new IllegalArgumentException( "You have to specify input sources for your application" );
        }
        return allSource;
    }

    /**
     * Sets the destination of this task.
     *
     * @param destinationDir The destination directory. Must not be null.
     */
    public void setDestinationDir( Object destinationDir ) {
        this.destinationDir = destinationDir;
    }

    /**
     * Vendor of this setup
     * @return vendor
     */
    @Input
    public String getVendor() {
        if( vendor != null ) {
            return vendor;
        }
        return "My Company";
    }

    /**
     * Set the vendor
     * @param vendor vendor
     */
    public void setVendor( String vendor ) {
        this.vendor = vendor;
    }

    /**
     * Return the name of the application
     * @return name of the application
     */
    @Input
    public String getApplication() {
        if( application != null ) {
            return application;
        }
        return project.getName();
    }

    /**
     * Setup an application with specified type
     * @param application type of application
     */
    public void setApplication( String application ) {
        this.application = application;
    }

    /**
     * Get the version of the setup. If not set a version of 1.0 is returned
     * @return the version
     */
    @Input
    public String getVersion() {
        if( version != null ) {
            return version;
        }
        Object version = project.getProperties().get( "version" );
        if( version != null ) {
            String strVersion = version.toString();
            if( !"unspecified".equalsIgnoreCase( strVersion ) ) {
                return strVersion;
            }
        }
        return "1.0";
    }

    /**
     * Set the version of the setup.
     * @param version the version
     */
    public void setVersion( String version ) {
        this.version = version;
    }

    /**
     * Get the short application identifier of the setup file. If not set then the project.archivesBaseName is used.
     * Should not contain spaces if possible.
     *
     * @return the application identifier
     */
    @Input
    public String getAppIdentifier() {
        if( appIdentifier != null ) {
            return appIdentifier;
        }
        Object name = project.getProperties().get( "archivesBaseName" );
        if( name != null ) {
            return name.toString();
        }
        return project.getName();
    }

    /**
     * Set the short application identifier for the setup file.
     * @param identifier new base name
     */
    public void setAppIdentifier( String identifier ) {
        this.appIdentifier = identifier;
    }

    /**
     * Get the name of the setup file without extension. If not set then baseName-version is used.
     *
     * @return the setup file name
     */
    @Input
    public String getArchiveName() {
        if( archiveName != null ) {
            return archiveName;
        }
        return getAppIdentifier() + '-' + getVersion();
    }

    /**
     * Set the name of the download file for the setup. This should contain the version if needed
     * @param setupName name of the setup
     */
    public void setArchiveName( String setupName ) {
        this.archiveName = setupName;
    }

    /**
     * Get the icons for the setup.
     * @return the icons
     */
    @Input
    @Optional
    public Object getIcons() {
        return icons;
    }

    /**
     * Retrieve a specific icon from the icons set for the setup
     * @param buildDir directory in which to put the icon
     * @param type of the icon to retrieve
     * @return the icon file
     * @throws IOException if an error occurs
     */
    @Internal
    public File getIconForType( File buildDir, String type ) throws IOException {
        return ImageFactory.getImageFile( project, getIcons(), buildDir, type);
    }

    /**
     * Set the icons for the setup. This can be one or multiple images in different size. The usage depends on the
     * platform. This can be an *.ico file, *.icns file or an list of Java readable image files like *.png or *.jpeg.
     *
     * @param icons the icons
     */
    public void setIcons( Object icons ) {
        this.icons = icons;
    }

    /**
     * Get the bundle JRE value.
     *
     * @return the value
     */
    @Input
    @Optional
    public Object getBundleJre() {
        return bundleJre;
    }

    /**
     * Add a Java VM into your setup. The resulting behavior depends on the platform. This can be a version or a
     * directory to a installed Java VM.
     *
     * @param bundleJre version or path
     * @see #setBundleJreTarget(String)
     */
    public void setBundleJre( Object bundleJre ) {
        this.bundleJre = bundleJre;
    }

    /**
     * Get the target for a bundle JRE.
     *
     * @return the target
     */
    @Input
    @Optional
    public String getBundleJreTarget() {
        if( bundleJreTarget != null ) {
            if( bundleJreTarget.startsWith( "/" ) ) {
                bundleJreTarget = bundleJreTarget.substring( 1 );
            }
            if( bundleJreTarget.endsWith( "/" ) ) {
                bundleJreTarget = bundleJreTarget.substring( 0, bundleJreTarget.length() - 1 );
            }
            return bundleJreTarget;
        }
        return "jre";
    }

    /**
     * The target directory in the install directory for a bundled JRE. The default is "jre".
     *
     * @param bundleJreTarget the new value
     */
    public void setBundleJreTarget( String bundleJreTarget ) {
        this.bundleJreTarget = bundleJreTarget;
    }

    /**
     * Get the main class.
     *
     * @return the class name
     */
    @Input
    @Optional
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Set the main class of the Java application.
     *
     * @param mainClass the class name
     */
    public void setMainClass( String mainClass ) {
        this.mainClass = mainClass;
    }

    /**
     * Get the main jar file.
     *
     * @return the main jar
     */
    @Input
    @Optional
    public String getMainJar() {
        return mainJar;
    }

    /**
     * Set the jar which contains the main class. This jar must contains all references to all other needed jar files in
     * the manifest.
     *
     * @param mainJar the main jar file
     */
    public void setMainJar( String mainJar ) {
        this.mainJar = mainJar;
    }

    /**
     * If enabled then an empty from definition is failing the build.
     *
     * @return true, if enabled
     */
    @Internal
    public boolean isFailOnEmptyFrom() {
        return failOnEmptyFrom;
    }

    /**
     * Enable the check.
     *
     * @param failOnEmptyFrom true, check is enabled
     */
    public void setFailOnEmptyFrom( boolean failOnEmptyFrom ) {
        this.failOnEmptyFrom = failOnEmptyFrom;
    }

    /**
     * Get a global description. Never null.
     * @return the description
     */
    @Input
    public String getDescription() {
        if( description == null ) {
            return "";
        }
        return description;
    }

    /**
     * Set a description for the installer. This is used for the packages but also as default for shortcuts, service, etc.
     * @param description new description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Get the Copyright information.
     * @return the copyright notice
     */
    @Input
    public String getCopyright() {
        if( copyright != null ) {
            return copyright;
        }
        StringBuilder builder = new StringBuilder( "© Copyright " );
        builder.append( Calendar.getInstance().get(Calendar.YEAR) );
        builder.append( " by " );
        builder.append( getVendor() );
        return builder.toString();
    }

    /**
     * Set the copyright information.
     * @param copyright the copyright notice
     */
    public void setCopyright( String copyright ) {
        this.copyright = copyright;
    }
}
