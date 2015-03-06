package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.IStreamableConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public class FileWriterComponent extends AbstractComponent {

	public static final String TYPE = "File Writer";
	
	private static final String FILE_TYPE_TEXT = "TEXT";
	private static final String FILE_TYPE_BINARY = "BINARY";
	private static final String DEFAULT_CHARSET = "UTF-8";
	
	@SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
	public final static String FILEWRITER_RELATIVE_PATH = "filewriter.relative.path";
	
	@SettingDefinition(order = 20, type = Type.CHOICE, choices = {
			FILE_TYPE_TEXT, FILE_TYPE_BINARY }, defaultValue = FILE_TYPE_TEXT, label = "File Type")
	public final static String FILEWRITER_FILE_TYPE = "filereader.file.type";

	@SettingDefinition(type = Type.BOOLEAN, order = 30, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String FILEWRITER_MUST_EXIST = "filewriter.must.exist";

	@SettingDefinition(type = Type.BOOLEAN, order = 40, required = true, provided = true, defaultValue = "false", label = "Append")
	public static final String FILEWRITER_APPEND = "filewriter.append";
	
	@SettingDefinition(type = Type.INTEGER, order = 50, label = "Line Terminator")
	public static final String FILEREADER_TEXT_LINE_TERMINATOR = "filereader.text.line.terminator";

	/* settings */
	String relativePathAndFile;
	String fileType;
	boolean mustExist;
	boolean append;
	String lineTerminator;

	/* other vars */
	TypedProperties properties;
	OutputStream outStream;
	BufferedWriter bufferedWriter=null;

	@Override
	public void start(IExecutionTracker executionTracker,
			IConnectionFactory connectionFactory) {
		super.start(executionTracker, connectionFactory);
		applySettings();
		outStream = getOutputStream((IStreamableConnection) this.connection
				.reference());
		if (fileType.equalsIgnoreCase(FILE_TYPE_TEXT)) {
			bufferedWriter = initializeWriter(outStream);
		}
	}

	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		
		/* we should get either an ArrayList of <String> for text file
		 * or byte[] for a binary file
		 */
		if (inputMessage.getPayload() instanceof byte[]) {
			handleBinaryFile(inputMessage, messageTarget);
		} else {
			handleTextFile(inputMessage, messageTarget);
		}		
	}

	@Override
	public void stop() {
		close();
		super.stop();
	}
		
	private void applySettings() {
		properties = componentNode.getComponentVersion().toTypedProperties(
				this, false);
		relativePathAndFile = properties.get(FILEWRITER_RELATIVE_PATH);
		fileType = properties.get(FILEWRITER_FILE_TYPE);
		mustExist = properties.is(FILEWRITER_MUST_EXIST);
		append = properties.is(FILEWRITER_APPEND);
		lineTerminator = properties.get(FILEREADER_TEXT_LINE_TERMINATOR);
	}

	private void handleBinaryFile(Message inputMessage, IMessageTarget messageTarget) {
		if (fileType.equalsIgnoreCase(FILE_TYPE_TEXT)) {
			throw new IoException("Converting from Binary input to Text output not implemented");
		}
		byte[] payload = (byte[]) inputMessage.getPayload();
		try {
			outStream.write(payload);
		} catch (IOException e) {
			throw new IoException("Error writing to file" + e.getMessage());
		}
	}
	
	private void handleTextFile(Message inputMessage,
			IMessageTarget messageTarget) {
		if (fileType.equalsIgnoreCase(FILE_TYPE_BINARY)) {
			throw new IoException(
					"Converting from Text input to Binary output not implemented");
		}
		ArrayList<String> recs = inputMessage.getPayload();
		try {
			for (String rec : recs) {
				bufferedWriter.write(rec);
			}
			bufferedWriter.flush();
		} catch (IOException e) {
			throw new IoException("Error writing to file " + e.getMessage());
		}
	}
	
	private OutputStream getOutputStream(IStreamableConnection conn) {
		conn.appendPath(relativePathAndFile,
				mustExist);
		return conn.getOutputStream();
	}

	private BufferedWriter initializeWriter(OutputStream stream) {
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					outStream, DEFAULT_CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new IoException("Error creating buffered writer "
					+ e.getMessage());
		}
		return bufferedWriter;
	}

	private void close() {
		try {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		} catch (IOException e) {
			throw new IoException("Failure in closing the writer "
					+ e.getMessage());
		} finally {
			closeStream();
		}
	}

	private void closeStream() {
		try {
			if (outStream != null) {
				outStream.close();
			}
		} catch (IOException e) {
			throw new IoException("Failure in closing the writer "
					+ e.getMessage());
		}
	}
}