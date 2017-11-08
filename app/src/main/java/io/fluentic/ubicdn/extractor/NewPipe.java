package io.fluentic.ubicdn.extractor;

/*
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * NewPipe.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.fluentic.ubicdn.extractor.exceptions.ExtractionException;

/**
 * Provides access to streaming services supported by NewPipe.
 */
public class NewPipe {
    private static final String TAG = NewPipe.class.toString();
    private static Downloader downloader = null;

    private NewPipe() {
    }

    public static void init(Downloader d) {
        downloader = d;
    }

    public static Downloader getDownloader() {
        return downloader;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public static StreamingService[] getServices() {
        final ServiceList[] values = ServiceList.values();
        final StreamingService[] streamingServices = new StreamingService[values.length];

        for (int i = 0; i < values.length; i++) streamingServices[i] = values[i].getService();

        return streamingServices;
    }

    public static StreamingService getService(int serviceId) throws ExtractionException {
        for (ServiceList item : ServiceList.values()) {
            if (item.getService().getServiceId() == serviceId) {
                return item.getService();
            }
        }
        throw new ExtractionException("There's no service with the id = \"" + serviceId + "\"");
    }

    public static StreamingService getService(String serviceName) throws ExtractionException {
        for (ServiceList item : ServiceList.values()) {
            if (item.getService().getServiceInfo().name.equals(serviceName)) {
                return item.getService();
            }
        }
        throw new ExtractionException("There's no service with the name = \"" + serviceName + "\"");
    }

    public static StreamingService getServiceByUrl(String url) throws ExtractionException {
        for (ServiceList item : ServiceList.values()) {
            if (item.getService().getLinkTypeByUrl(url) != StreamingService.LinkType.NONE) {
                return item.getService();
            }
        }
        throw new ExtractionException("No service can handle the url = \"" + url + "\"");
    }

    public static int getIdOfService(String serviceName) {
        try {
            //noinspection ConstantConditions
            return getService(serviceName).getServiceId();
        } catch (ExtractionException ignored) {
            return -1;
        }
    }

    public static String getNameOfService(int id) {
        try {
            //noinspection ConstantConditions
            return getService(id).getServiceInfo().name;
        } catch (Exception e) {
            System.err.println("Service id not known");
            e.printStackTrace();
            return "<unknown>";
        }
    }
}
