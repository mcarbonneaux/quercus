/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.TempBufferStringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Construct;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StringStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.Html;
import com.caucho.xml.QDocument;
import com.caucho.xml.QDocumentType;
import com.caucho.xml.QName;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlPrinter;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DOMDocument
  extends QDocument
{
  private final static Logger log = Logger.getLogger(DOMDocument.class.getName());

  private String _version;
  private String _encoding;

  @Construct
  public DOMDocument(@Optional("'1.0'") String version, @Optional String encoding)
  {
    _version = version;

    if (encoding != null && encoding.length() > 0)
      setEncoding(encoding);
  }

  DOMDocument(DOMImplementation implementation)
  {
    super(implementation.getQDOMImplementation());
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  public String getVersion()
  {
    return _version;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public DOMConfiguration getConfig()
  {
    throw new UnimplementedException();
  }

  public boolean getFormatOutput()
  {
    throw new UnimplementedException();
  }

  public void setFormatOutput(boolean formatOutput)
  {
    throw new UnimplementedException();
  }

  public boolean getPreserveWhiteSpace()
  {
    throw new UnimplementedException();
  }

  public void setPreserveWhiteSpace(boolean preserveWhiteSpace)
  {
    throw new UnimplementedException();
  }

  public boolean getRecover()
  {
    throw new UnimplementedException();
  }

  public void setRecover(boolean recover)
  {
    throw new UnimplementedException();
  }

  public boolean getResolveExternals()
  {
    throw new UnimplementedException();
  }

  public void setResolveExternals(boolean resolveExternals)
  {
    throw new UnimplementedException();
  }

  public boolean getSubstituteEntities()
  {
    throw new UnimplementedException();
  }

  public void setSubstituteEntities(boolean substituteEntities)
  {
    throw new UnimplementedException();
  }

  public boolean getValidateOnParse()
  {
    throw new UnimplementedException();
  }

  public void setValidateOnParse(boolean validateOnParse)
  {
    throw new UnimplementedException();
  }

  public DOMAttr createAttribute(String name)
    throws DOMException
  {
    /** XXX:
     if (! isNameValid(name))
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     "illegal attribute `" + name + "'");
     */

    QName qname = new QName(null, name, null);

    return new DOMAttr(this, qname);
  }

  public DOMAttr createAttributeNS(String namespaceURI, String qualifiedName)
    throws DOMException
  {
    QName qname = createName(namespaceURI, qualifiedName);

    validateName(qname);
    addNamespace(qname);

    return new DOMAttr(this, qname);
  }
  public DOMCDATASection createCDATASection(String data)
  {
    return new DOMCDATASection(this, data);
  }

  public DOMComment createComment(String data)
  {
    return new DOMComment(this, data);
  }

  public DOMDocumentFragment createDocumentFragment()
  {
    return new DOMDocumentFragment(this);
  }

  public DOMElement createElement(String tagName)
    throws DOMException
  {
    /**
     if (! isNameValid(tagName))
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     "illegal tag `" + tagName + "'");
     */
    return new DOMElement(this, createName(null, tagName), null);
  }

  public DOMElement createElement(String tagName, String textContent)
    throws DOMException
  {
    /**
     if (! isNameValid(tagName))
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     "illegal tag `" + tagName + "'");
     */
    return new DOMElement(this, createName(null, tagName), textContent);
  }

  public DOMElement createElementNS(String namespaceURI, String name)
    throws DOMException
  {
    QName qname = createName(namespaceURI, name);

    validateName(qname);
    addNamespace(qname);

    return new DOMElement(this, qname, null);
  }

  public DOMElement createElementNS(String namespaceURI,
                                    String name,
                                    String textContent)
    throws DOMException
  {
    QName qname = createName(namespaceURI, name);

    validateName(qname);
    addNamespace(qname);

    return new DOMElement(this, qname, textContent);
  }

  public DOMEntityReference createEntityReference(String name)
    throws DOMException
  {
    /**
     if (! isNameValid(name))
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     "illegal entityReference `" + name + "'");
     */

    return new DOMEntityReference(this, name);
  }

  public DOMProcessingInstruction createProcessingInstruction(String target)
    throws DOMException
  {
    return createProcessingInstruction(target, null);
  }

  public DOMProcessingInstruction createProcessingInstruction(String target,
                                                              String data)
    throws DOMException
  {
    /** XXX:
     if (target == null || target.length() == 0)
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     L.l("Empty processing instruction name.  The processing instruction syntax is: <?name ... ?>"));

     if (! isNameValid(target))
     throw new QDOMException(DOMException.INVALID_CHARACTER_ERR,
     L.l("`{0}' is an invalid processing instruction name.  The processing instruction syntax is: <?name ... ?>", target));
     */

    return new DOMProcessingInstruction(this, target, data);
  }

  public DOMText createTextNode(String data)
  {
    return new DOMText(this, data);
  }

  public Node importNode(Node node)
  {
    throw new UnimplementedException();
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean load(Env env, Path path, @Optional Value options)
  {
    if (options != null)
      env.stub(L.l("`{0}' is ignored", "options"));

    ReadStream is = null;

    try {
      is = path.openRead();

      Xml xml = new Xml();

      xml.parseDocument(this, is, path.getPath());
    }
    catch (SAXException ex) {
      env.warning(ex);

      return false;
    }
    catch (IOException ex) {
      env.warning(ex);
      return false;
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return true;
  }

  /**
   * @param source A string containing the HTML
   */
  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadHTML(Env env, String source)
  {
    ReadStream is = StringStream.open(source);

    try {
      Html html = new Html();

      html.parseDocument(this, is, null);

      setStandalone(true);
      setDoctype(new QDocumentType("html",
                                   "-//W3C//DTD HTML 4.0 Transitional//EN",
                                   "http://www.w3.org/TR/REC-html40/loose.dtd"));
    }
    catch (SAXException ex) {
      env.warning(ex);
      return false;
    }
    catch (IOException ex) {
      env.warning(ex);
      return false;
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return true;
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadHTMLFile(Env env, Path path)
  {
    ReadStream is = null;

    try {
      is = path.openRead();

      Html html = new Html();

      html.parseDocument(this, is, path.getPath());

      setStandalone(true);
      setDoctype(new QDocumentType("html",
                                   "-//W3C//DTD HTML 4.0 Transitional//EN",
                                   "http://www.w3.org/TR/REC-html40/loose.dtd"));
    }
    catch (SAXException ex) {
      env.warning(ex);
      return false;
    }
    catch (IOException ex) {
      env.warning(ex);
      return false;
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return true;
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadXML(Env env, String source, @Optional Value options)
  {
    if (options != null)
      env.stub(L.l("`{0}' is ignored", "options"));

    ReadStream is = StringStream.open(source);

    try {
      Xml xml = new Xml();

      xml.parseDocument(this, is, null);
    }
    catch (SAXException ex) {
      env.warning(ex);

      return false;
    }
    catch (IOException ex) {
      env.warning(ex);
      return false;
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return true;
  }

  public boolean relaxNGValidate(String rngFilename)
  {
    throw new UnimplementedException();
  }

  public boolean relaxNGValidateSource(String rngSource)
  {
    throw new UnimplementedException();
  }

  private void saveToStream(WriteStream os, boolean isHTML)
    throws IOException
  {
    XmlPrinter printer = new XmlPrinter(os);

    printer.setMethod(isHTML ? "html" : "xml");

    printer.setVersion(_version);
    printer.setEncoding(_encoding);

    printer.setPrintDeclaration(true);

    if (getStandalone())
      printer.setStandalone("yes");

    printer.printXml(this);

    os.println();
  }

  private Value saveToFile(Env env, Path path, boolean isHTML)
  {
    WriteStream os = null;

    try {
      os = path.openWrite();
      saveToStream(os, isHTML);

    }
    catch (IOException ex) {
      env.warning(ex);
      return BooleanValue.FALSE;
    }
    finally {
      if (os != null) {
        try {
          os.close();
        }
        catch (Exception ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return new LongValue(path.getLength());
  }

  private StringValue saveToString(Env env, boolean isHTML)
  {
    TempStream tempStream = new TempStream();

    try {
      tempStream.openWrite();
      WriteStream os = new WriteStream(tempStream);

      saveToStream(os, isHTML);

      os.close();
    }
    catch (IOException ex) {
      tempStream.discard();
      env.warning(ex);
      return null;
    }

    TempBufferStringValue result = new TempBufferStringValue(tempStream.getHead());

    tempStream.discard();

    return result;
  }

  /**
   * @return the number of bytes written, or FALSE for an error
   */
  public Value save(Env env, Path path, @Optional Value options)
  {
    if (options != null)
      env.stub(L.l("`{0}' is ignored", "options"));

    return saveToFile(env, path, false);
  }

  @ReturnNullAsFalse
  public StringValue saveHTML(Env env)
  {
    return saveToString(env, true);
  }

  /**
   * @return the number of bytes written, or FALSE for an error
   */

  public Value saveHTMLFile(Env env, Path path)
  {
    return saveToFile(env, path, true);
  }

  @ReturnNullAsFalse
  public StringValue saveXML(Env env)
  {
    return saveToString(env, false);
  }

  public boolean schemaValidate(String schemaFilename)
  {
    throw new UnimplementedException();
  }

  public boolean schemaValidateSource(String schemaSource)
  {
    throw new UnimplementedException();
  }

  public boolean validate()
  {
    throw new UnimplementedException();
  }

  public int xinclude(Env env, @Optional Value options)
  {
    if (options != null)
      env.stub(L.l("`{0}' is ignored", "options"));

    throw new UnimplementedException();
  }

  public String toString()
  {
    return "DOMDocument[]";
  }
}
