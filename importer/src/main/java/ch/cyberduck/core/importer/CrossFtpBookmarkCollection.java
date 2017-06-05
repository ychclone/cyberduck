package ch.cyberduck.core.importer;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.dav.DAVProtocol;
import ch.cyberduck.core.dav.DAVSSLProtocol;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.s3.S3Protocol;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

public class CrossFtpBookmarkCollection extends XmlBookmarkCollection {
    private static final Logger log = Logger.getLogger(CrossFtpBookmarkCollection.class);

    private static final long serialVersionUID = 7442378118872253601L;

    @Override
    public String getBundleIdentifier() {
        return "com.crossftp";
    }

    @Override
    public String getName() {
        return "CrossFTP";
    }

    @Override
    public Local getFile() {
        return LocalFactory.get(PreferencesFactory.get().getProperty("bookmark.import.crossftp.location"));
    }

    @Override
    protected AbstractHandler getHandler() {
        return new ServerHandler();
    }

    /**
     * Parser for Filezilla Site Manager.
     */
    private class ServerHandler extends AbstractHandler {
        private Host current = null;

        @Override
        public void startElement(String name, Attributes attrs) {
            switch(name) {
                case "site":
                    current = new Host(ProtocolFactory.forScheme(Scheme.ftp), attrs.getValue("hName"));
                    current.setNickname(attrs.getValue("name"));
                    current.getCredentials().setUsername(attrs.getValue("un"));
                    current.setWebURL(attrs.getValue("wURL"));
                    current.setComment(attrs.getValue("comm"));
                    current.setDefaultPath(attrs.getValue("path"));
                    String protocol = attrs.getValue("ftpPType");
                    try {
                        switch(Integer.valueOf(protocol)) {
                            case 1:
                                current.setProtocol(ProtocolFactory.forScheme(Scheme.ftp));
                                break;
                            case 2:
                            case 3:
                            case 4:
                                current.setProtocol(ProtocolFactory.forScheme(Scheme.ftps));
                                break;
                            case 6:
                                current.setProtocol(ProtocolFactory.forScheme(new DAVProtocol().getIdentifier()));
                                break;
                            case 7:
                                current.setProtocol(ProtocolFactory.forScheme(new DAVSSLProtocol().getIdentifier()));
                                break;
                            case 8:
                            case 9:
                                current.setProtocol(ProtocolFactory.forScheme(new S3Protocol().getIdentifier()));
                                break;
                        }
                        // Reset port to default
                        current.setPort(-1);
                    }
                    catch(NumberFormatException e) {
                        log.warn("Unknown protocol:" + e.getMessage());
                    }
                    try {
                        current.setPort(Integer.parseInt(attrs.getValue("port")));
                    }
                    catch(NumberFormatException e) {
                        log.warn("Invalid Port:" + e.getMessage());
                    }
                    break;
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            switch(name) {
                case "site":
                    add(current);
                    break;
            }
        }
    }
}