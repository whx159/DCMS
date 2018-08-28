package com.huateng.hsbc.report.service;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.export.JRExportProgressMonitor;
import net.sf.jasperreports.engine.export.JRTextExporterContext;
import net.sf.jasperreports.engine.util.JRStyledText;
import net.sf.jasperreports.export.ExporterInputItem;
import net.sf.jasperreports.export.TextExporterConfiguration;
import net.sf.jasperreports.export.TextReportConfiguration;
import net.sf.jasperreports.export.WriterExporterOutput;
import net.sf.jasperreports.export.parameters.ParametersWriterExporterOutput;

public class JRTextExporter
  extends JRAbstractExporter<TextReportConfiguration, TextExporterConfiguration, WriterExporterOutput, JRTextExporterContext>
{
  private static final String TXT_EXPORTER_PROPERTIES_PREFIX = "net.sf.jasperreports.export.txt.";
  protected Writer writer;
  char[][] pageData;
  protected int pageWidthInChars;
  protected int pageHeightInChars;
  protected float charWidth;
  protected float charHeight;
  protected String pageSeparator;
  protected String lineSeparator;
  protected static final String systemLineSeparator = System.getProperty("line.separator");
  
  protected class ExporterContext
    extends JRAbstractExporter<TextReportConfiguration, TextExporterConfiguration, WriterExporterOutput, JRTextExporterContext>.BaseExporterContext
    implements JRTextExporterContext
  {
    protected ExporterContext()
    {
      super();
    }
  }
  
  public JRTextExporter()
  {
    this(DefaultJasperReportsContext.getInstance());
  }
  
  public JRTextExporter(JasperReportsContext jasperReportsContext)
  {
    super(jasperReportsContext);
    
    this.exporterContext = new ExporterContext();
  }
  
  protected Class<TextExporterConfiguration> getConfigurationInterface()
  {
    return TextExporterConfiguration.class;
  }
  
  protected Class<TextReportConfiguration> getItemConfigurationInterface()
  {
    return TextReportConfiguration.class;
  }
  
  protected void ensureOutput()
  {
    if (this.exporterOutput == null) {
      this.exporterOutput = 
        new ParametersWriterExporterOutput(
        getJasperReportsContext(), 
        getParameters(), 
        getCurrentJasperPrint());
    }
  }
  
  public void exportReport()
    throws JRException
  {
    ensureJasperReportsContext();
    ensureInput();
    
    initExport();
    
    ensureOutput();
    
    this.writer = ((WriterExporterOutput)getExporterOutput()).getWriter();
    try
    {
      exportReportToWriter();
    }
    catch (IOException e)
    {
      throw new JRException("Error writing to output writer : " + this.jasperPrint.getName(), e);
    }
    finally
    {
      ((WriterExporterOutput)getExporterOutput()).close();
    }
  }
  
  protected void initExport()
  {
    super.initExport();
    
    TextExporterConfiguration configuration = (TextExporterConfiguration)getCurrentConfiguration();
    
    this.lineSeparator = configuration.getLineSeparator();
    if (this.lineSeparator == null) {
      this.lineSeparator = systemLineSeparator;
    }
    this.pageSeparator = configuration.getPageSeparator();
    if (this.pageSeparator == null) {
      this.pageSeparator = (systemLineSeparator + systemLineSeparator);
    }
  }
  
  protected void initReport()
  {
    super.initReport();
    
    TextReportConfiguration configuration = (TextReportConfiguration)getCurrentItemConfiguration();
    
    Float charWidthValue = configuration.getCharWidth();
    this.charWidth = (charWidthValue == null ? 0.0F : charWidthValue.floatValue());
    if (this.charWidth < 0.0F) {
      throw new JRRuntimeException("Character width in pixels must be greater than zero.");
    }
    if (this.charWidth == 0.0F)
    {
      Integer pageWidthInCharsValue = configuration.getPageWidthInChars();
      this.pageWidthInChars = (pageWidthInCharsValue == null ? 0 : pageWidthInCharsValue.intValue());
      if (this.pageWidthInChars <= 0) {
        throw new JRRuntimeException("Character width in pixels or page width in characters must be specified and must be greater than zero.");
      }
      this.charWidth = (this.jasperPrint.getPageWidth() / this.pageWidthInChars);
    }
    else
    {
      this.pageWidthInChars = ((int)(this.jasperPrint.getPageWidth() / this.charWidth) +1);
    }
    Float charHeightValue = configuration.getCharHeight();
    this.charHeight = (charHeightValue == null ? 0.0F : charHeightValue.floatValue());
    if (this.charHeight < 0.0F) {
      throw new JRRuntimeException("Character height in pixels must be greater than zero.");
    }
    if (this.charHeight == 0.0F)
    {
      Integer pageHeightInCharsValue = configuration.getPageHeightInChars();
      this.pageHeightInChars = (pageHeightInCharsValue == null ? 0 : pageHeightInCharsValue.intValue());
      if (this.pageHeightInChars <= 0) {
        throw new JRRuntimeException("Character height in pixels or page height in characters must be specified and must be greater than zero.");
      }
      this.charHeight = (this.jasperPrint.getPageHeight() / this.pageHeightInChars);
    }
    else
    {
      this.pageHeightInChars = ((int)(this.jasperPrint.getPageHeight() / this.charHeight)+1);
    }
  }
  
  protected void exportReportToWriter()
    throws JRException, IOException
  {
    List<ExporterInputItem> items = this.exporterInput.getItems();
    for (int reportIndex = 0; reportIndex < items.size(); reportIndex++)
    {
      ExporterInputItem item = (ExporterInputItem)items.get(reportIndex);
      
      setCurrentExporterInputItem(item);
      
      List<JRPrintPage> pages = this.jasperPrint.getPages();
      if ((pages != null) && (pages.size() > 0))
      {
        JRAbstractExporter<TextReportConfiguration, TextExporterConfiguration, WriterExporterOutput, JRTextExporterContext>.PageRange pageRange = getPageRange();
        int startPageIndex = (pageRange == null) || (pageRange.getStartPageIndex() == null) ? 0 : pageRange.getStartPageIndex().intValue();
        int endPageIndex = (pageRange == null) || (pageRange.getEndPageIndex() == null) ? pages.size() - 1 : pageRange.getEndPageIndex().intValue();
        for (int i = startPageIndex; i <= endPageIndex; i++)
        {
          if (Thread.interrupted()) {
            throw new JRException("Current thread interrupted.");
          }
          JRPrintPage page = (JRPrintPage)pages.get(i);
          

          exportPage(page);
        }
      }
    }
    this.writer.flush();
  }
  
  protected void exportPage(JRPrintPage page)
    throws IOException
  {
    List<JRPrintElement> elements = page.getElements();
    
    this.pageData = new char[this.pageHeightInChars][];
    for (int i = 0; i < this.pageHeightInChars; i++)
    {
      this.pageData[i] = new char[this.pageWidthInChars];
      Arrays.fill(this.pageData[i], ' ');
    }
    exportElements(elements);
    for (int i = 0; i < this.pageHeightInChars; i++)
    {
      this.writer.write(this.pageData[i]);
      this.writer.write(this.lineSeparator);
    }
    this.writer.write(this.pageSeparator);
    
    JRExportProgressMonitor progressMonitor = ((TextReportConfiguration)getCurrentItemConfiguration()).getProgressMonitor();
    if (progressMonitor != null) {
      progressMonitor.afterPageExport();
    }
  }
  
  protected void exportElements(List<JRPrintElement> elements)
  {
    for (int i = 0; i < elements.size(); i++)
    {
      Object element = elements.get(i);
      if ((element instanceof JRPrintText))
      {
        exportText((JRPrintText)element);
      }
      else if ((element instanceof JRPrintFrame))
      {
        JRPrintFrame frame = (JRPrintFrame)element;
        setFrameElementsOffset(frame, false);
        try
        {
          exportElements(frame.getElements());
        }
        finally
        {
          restoreElementOffsets();
        }
      }
    }
  }
  
  protected void exportText(JRPrintText element)
  {
    int colSpan = getWidthInChars(element.getWidth());
    int rowSpan = getHeightInChars(element.getHeight());
    int col = getWidthInChars(element.getX() + getOffsetX());
    int row = getHeightInChars(element.getY() + getOffsetY());
    if (col + colSpan > this.pageWidthInChars) {
      colSpan = this.pageWidthInChars - col;
    }
    JRStyledText styledText = getStyledText(element);
    String allText;
//    String allText;
    if (styledText == null) {
      allText = "";
    } else {
      allText = styledText.getText();
    }
    if ((rowSpan <= 0) || (colSpan <= 0)) {
      return;
    }
    if ((allText != null) && (allText.length() == 0)) {
      return;
    }
    StringBuffer[] rows = new StringBuffer[rowSpan];
    rows[0] = new StringBuffer();
    int rowIndex = 0;
    int rowPosition = 0;
    boolean isFirstLine = true;
    

    StringTokenizer lfTokenizer = new StringTokenizer(allText, "\n", true);
    while (lfTokenizer.hasMoreTokens())
    {
      String line = lfTokenizer.nextToken();
      if ((isFirstLine) && (line.equals("\n")))
      {
        rows[rowIndex].append("");
        rowIndex++;
        if ((rowIndex == rowSpan) || (!lfTokenizer.hasMoreTokens())) {
          break;
        }
        rowPosition = 0;
        rows[rowIndex] = new StringBuffer();
        line = lfTokenizer.nextToken();
      }
      isFirstLine = false;
      

      int emptyLinesCount = 0;
      while ((line.equals("\n")) && (lfTokenizer.hasMoreTokens()))
      {
        emptyLinesCount++;
        line = lfTokenizer.nextToken();
      }
      if (emptyLinesCount > 1) {
        for (int i = 0; i < emptyLinesCount - 1; i++)
        {
          rows[rowIndex].append("");
          rowIndex++;
          if (rowIndex == rowSpan) {
            break;
          }
          rowPosition = 0;
          rows[rowIndex] = new StringBuffer();
          if ((!lfTokenizer.hasMoreTokens()) && (line.equals("\n")))
          {
            rows[rowIndex].append("");
            break;
          }
        }
      }
      if (!line.equals("\n"))
      {
        StringTokenizer spaceTokenizer = new StringTokenizer(line, " ", true);
        while (spaceTokenizer.hasMoreTokens())
        {
          String word = spaceTokenizer.nextToken();
          while (word.length() > colSpan)
          {
            rows[rowIndex].append(word.substring(0, colSpan - rowPosition));
            word = word.substring(colSpan - rowPosition, word.length());
            rowIndex++;
            if (rowIndex == rowSpan) {
              break;
            }
            rowPosition = 0;
            rows[rowIndex] = new StringBuffer();
          }
          if (rowPosition + word.length() > colSpan)
          {
            rowIndex++;
            if (rowIndex == rowSpan) {
              break;
            }
            rowPosition = 0;
            rows[rowIndex] = new StringBuffer();
          }
          if ((rowIndex <= 0) || (rowPosition != 0) || (!word.equals(" ")))
          {
            rows[rowIndex].append(word);
            rowPosition += word.length();
          }
        }
        rowIndex++;
        if (rowIndex == rowSpan) {
          break;
        }
        rowPosition = 0;
        rows[rowIndex] = new StringBuffer();
      }
    }
    int colOffset = 0;
    int rowOffset = 0;
    switch (element.getVerticalAlignmentValue())
    {
    case MIDDLE: 
      rowOffset = rowSpan - rowIndex;
      break;
    case JUSTIFIED: 
      rowOffset = (rowSpan - rowIndex) / 2;
    }
    for (int i = 0; i < rowIndex; i++)
    {
      String line = rows[i].toString();
      int pos = line.length() - 1;
      while ((pos >= 0) && (line.charAt(pos) == ' ')) {
        pos--;
      }
      line = line.substring(0, pos + 1);
      switch (element.getHorizontalAlignmentValue())
      {
      case LEFT: 
        colOffset = colSpan - line.length();
        break;
      case JUSTIFIED: 
        colOffset = (colSpan - line.length()) / 2;
        break;
      case RIGHT: 
        if (i < rowIndex - 1) {
          line = justifyText(line, colSpan);
        }
        break;
      }
      char[] chars = line.toCharArray();
      System.out.println("---------------------"+ (row + rowOffset + i));
      if((row + rowOffset + i) == 11){
    	  System.out.println((row + rowOffset + i));
      }
      System.out.println("---------------------"+this.pageData[(row + rowOffset + i)].length);
      System.out.println("---------------------chars"+chars.length);
      System.arraycopy(chars, 0, this.pageData[(row + rowOffset + i)], col + colOffset, chars.length);
    }
  }
  
  private String justifyText(String s, int width)
  {
    StringBuffer justified = new StringBuffer();
    
    StringTokenizer t = new StringTokenizer(s, " ");
    int tokenCount = t.countTokens();
    if (tokenCount <= 1) {
      return s;
    }
    String[] words = new String[tokenCount];
    int i = 0;
    while (t.hasMoreTokens()) {
      words[(i++)] = t.nextToken();
    }
    int emptySpace = width - s.length() + (words.length - 1);
    int spaceCount = emptySpace / (words.length - 1);
    int remainingSpace = emptySpace % (words.length - 1);
    
    char[] spaces = new char[spaceCount];
    Arrays.fill(spaces, ' ');
    for (i = 0; i < words.length - 1; i++)
    {
      justified.append(words[i]);
      justified.append(spaces);
      if (i < remainingSpace) {
        justified.append(' ');
      }
    }
    justified.append(words[(words.length - 1)]);
    
    return justified.toString();
  }
  
  protected int getHeightInChars(int height)
  {
    return Math.round(height / this.charHeight);
  }
  
  protected int getWidthInChars(int width)
  {
    return Math.round(width / this.charWidth);
  }
  
  protected JRStyledText getStyledText(JRPrintText textElement)
  {
    return this.styledTextUtil.getStyledText(textElement, this.noneSelector);
  }
  
  public String getExporterKey()
  {
    return null;
  }
  
  public String getExporterPropertiesPrefix()
  {
    return "net.sf.jasperreports.export.txt.";
  }
}
