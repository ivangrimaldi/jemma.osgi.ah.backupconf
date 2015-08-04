/**
 * This file is part of JEMMA - http://jemma.energy-home.org
 * (C) Copyright 2013 Telecom Italia (http://www.telecomitalia.it)
 *
 * JEMMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) version 3
 * or later as published by the Free Software Foundation, which accompanies
 * this distribution and is available at http://www.gnu.org/licenses/lgpl.html
 *
 * JEMMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License (LGPL) for more details.
 *
 */
package org.energy_home.jemma.osgi.ah.backupmng;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import org.slf4j.*;
import org.energy_home.jemma.ah.configurator.*;
import org.osgi.framework.*;
import org.osgi.service.http.*;

public class BackupConf extends HttpServlet {

	private HttpService _httpService = null;
	private static final String rootHttpAlias = "/backupconfig";
	private IConfigurator _configurator = null;
	
	private static Logger log = LoggerFactory.getLogger(BackupConf.class);
	
	private String configurationFilename = "configuration";

	public BackupConf() {
	}

	public void activate(BundleContext bc) {
	}

	public void deactivate() {
	}
	
	public void bindHttpService(HttpService httpService) throws Exception {
		_httpService = httpService;
		_httpService.registerResources(rootHttpAlias, "webapp", null);
		_httpService.registerServlet(rootHttpAlias + "/servlet", this, null, null);
	}

	public void unbindHttpService(HttpService httpService) {
		_httpService = null;
	}

	public void bindConfigurator(IConfigurator configurator) {
		_configurator = configurator;
	}

	public void unbindConfigurator(IConfigurator configurator) {
		_configurator = null;
	}
	
	private boolean _exportConfiguration(HttpServletResponse res) {
		res.addHeader("Content-Type", "text/xml");
		
		//res.setContentType("text/xml");
		res.addHeader("Content-disposition", "attachment; filename=" + this.configurationFilename + ".xml");
		res.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		res.addHeader("Cache-Control", "post-check=0, pre-check=0");
		res.addHeader("Pragma", "no-cache");

		try {
			_configurator.exportConfiguration(res.getOutputStream());
		} catch (Exception e) {
			log.error("Exception exporting configuration",e);
			return (false);
		}

		return (true);
	}

	private boolean _importConfiguration(FileItem fileItem) {
		try {
			// Applica la configurazione
			_configurator.importConfiguration(fileItem.getInputStream());
		} catch (Exception e) {
			log.error("Exception importing configuration",e);
			return (false);
		}

		return (true);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());

		try {
			// Parsifica la richiesta HTTP...
			List<FileItem> fileItems;
			fileItems = servletFileUpload.parseRequest(req);

			for (FileItem fileItem : fileItems) {
				// E' una richiesta di export...
				if (fileItem.isFormField() == true) {
					if (fileItem.getFieldName().equals("btnexport") == true) {
						_exportConfiguration(res);
						break;
					}
				} else {
					// ...oppure di import!
					if (_importConfiguration(fileItem) == true)
						res.sendRedirect(rootHttpAlias + "/importok.html");
					else
						res.sendRedirect(rootHttpAlias + "/importerror.html");
					break;
				}
			}
		} catch (Exception e) {
			log.error("Exception pasing request",e);
		}
	}
}