/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.report.engine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.apps.tool.net.URLService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;

public class ExternalReportSettings extends ReportSettings  {
  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected static String BIRT_PATH = "birt";
  protected String url = "";
  protected String birtViewerUrl = null;;

  public ExternalReportSettings(String rptdesign, String outputName)  {

    super(rptdesign, outputName);

    this.addAxelorReportPath(rptdesign)
    .addParam("__locale", AppFilter.getLocale().toString());

  }


  @Override
  public ExternalReportSettings generate() throws AxelorException  {

    super.generate();

    try  {
      this.getUrl();

      String urlNotExist = URLService.notExist(url.toString());
      if (urlNotExist != null) {
        throw new AxelorException(TraceBackRepository.CATEGORY_CONFIGURATION_ERROR, I18n.get(com.axelor.apps.base.exceptions.IExceptionMessage.BIRT_EXTERNAL_REPORT_NO_URL), birtViewerUrl);
      }

      final Path tmpFile = MetaFiles.createTempFile(null, "");

      this.output = tmpFile.toFile();

      URLService.fileDownload(this.output, url, "", outputName);

      this.attach();

    }  catch(IOException ioe)  {
      throw new AxelorException(ioe, TraceBackRepository.CATEGORY_CONFIGURATION_ERROR);
    }

    return this;
  }

  public String getUrl()  {

    addParam("__format", format);

    for(String param : params.keySet())  {

      try {
        this.url +=  this.computeParam(param);
      } catch (UnsupportedEncodingException e) {
        logger.error(e.getLocalizedMessage());
      }

    }

    return this.url;

  }	

  private String computeParam(String param) throws UnsupportedEncodingException  {

    return "&" + param + "=" + URLEncoder.encode(params.get(param).toString(), "UTF-8");

  }


  private ReportSettings addAxelorReportPath(String rptdesign)  {

    AppSettings appsSettings = AppSettings.get();

    String defaultUrl = appsSettings.getBaseURL();
    defaultUrl = defaultUrl.substring(0, defaultUrl.lastIndexOf('/'));
    defaultUrl += "/" + BIRT_PATH ;

    this.birtViewerUrl = appsSettings.get("axelor.report.engine", defaultUrl);

    String resourcePath = appsSettings.get("axelor.report.resource.path", "report");
    resourcePath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";

    this.url +=  birtViewerUrl + "/frameset?__report=" + resourcePath + rptdesign;
    return this;

  }	

}
