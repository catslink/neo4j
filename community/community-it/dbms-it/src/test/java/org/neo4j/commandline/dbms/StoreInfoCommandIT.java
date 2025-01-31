/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.commandline.dbms;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.neo4j.commandline.admin.AdminTool;
import org.neo4j.commandline.admin.CommandFailed;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.StoreLockException;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StoreInfoCommandIT
{
    @Rule
    public final TestDirectory testDirectory = TestDirectory.testDirectory();

    @Test
    public void respectLockFilesSameProcess() throws Exception
    {
        GraphDatabaseService database = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( testDirectory.storeDir() ).newGraphDatabase();
        StoreInfoCommand command = new StoreInfoCommand( System.out::println );
        try
        {
            command.execute( new String[]{"--store", testDirectory.storeDir().getAbsolutePath()} );
            fail();
        }
        catch ( CommandFailed e )
        {
            assertTrue( e.getCause() instanceof StoreLockException );
        }
        finally
        {
            database.shutdown();
        }
    }

    @Test
    public void respectLockFiles() throws Exception
    {
        GraphDatabaseService database = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( testDirectory.storeDir() ).newGraphDatabase();
        Process process = runInSeparateProcess( testDirectory.storeDir().getAbsolutePath() );
        assertEquals( 1, process.waitFor() );
        assertTrue( new Scanner( process.getErrorStream() ).nextLine().contains( "the database is in use" ) );
        database.shutdown();
    }

    private Process runInSeparateProcess( String storeDirectory ) throws Exception
    {
        String classpath = Arrays.stream( ((URLClassLoader) Thread.currentThread().getContextClassLoader())
                .getURLs() )
                .map( URL::getFile )
                .collect( Collectors.joining( File.pathSeparator ) );
        return new ProcessBuilder(
                System.getProperty( "java.home" ) + "/bin/java",
                "-classpath", classpath,
                AdminTool.class.getName(), "store-info",
                "--store", storeDirectory
        ).start();
    }
}
