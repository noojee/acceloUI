package au.com.noojee.acceloUI.forms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.activation.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.easyuploads.FileBuffer;
import org.vaadin.easyuploads.MultiFileUpload;

import com.vaadin.data.Binder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloUI.util.ButtonEventSource;
import au.com.noojee.acceloUI.util.SMNotification;
import rx.Subscription;
import rx.util.functions.Action1;

public class EmailForm extends Window
{

	@SuppressWarnings("unused")
	private static Logger logger = LogManager.getLogger(EmailForm.class);
	private static final long serialVersionUID = 1L;
	private static final String TEMP_FILE_DIR = new File(System.getProperty("java.io.tmpdir")).getPath();

	private final UserProvider sender;
	private final ContactProvider contactProvider;

	private final TextField subject;
	private final CKEditorEmailField ckEditor;
	private final Button send;
	private final VerticalLayout attachedFiles;
	private final HashSet<AttachedFileLayout> fileList = new HashSet<>();

	private final GridLayout grid;

	class TargetLine
	{
		ComboBox<EmailAddressType> targetTypeCombo;
		ComboBox<Contact> targetAddress;
		Button minusButton;
		private Subscription buttonSubscription;
		public int row;
		public Binder<Contact> targetAddressBinder;
	}

	ArrayList<TargetLine> lines = new ArrayList<>();
	private final ComboBox<EmailAddressType> primaryTypeCombo;
	private final TextField primaryTargetAddress;
	private final Button primaryPlusButton;
	private String body;

	/**
	 *
	 * @param sender
	 *            the user who is sending this email.
	 */
	public EmailForm(final UserProvider sender, final ContactProvider contactProvider, final String toEmailAddress)
	{

		this.sender = sender;
		this.contactProvider = contactProvider;

		this.setWidth("80%");
		this.setHeight("80%");
		this.center();

		VerticalLayout baseLayout = new VerticalLayout();
		this.setContent(baseLayout);
		baseLayout.setSpacing(true);
		baseLayout.setMargin(true);
		baseLayout.setSizeFull();

		this.grid = new GridLayout(4, 2);
		baseLayout.addComponent(this.grid);

		this.grid.setWidth("100%");
		this.grid.setColumnExpandRatio(1, (float) 1.0);
		this.grid.setSpacing(true);

		final List<EmailAddressType> targetTypes = getTargetTypes();

		this.primaryTypeCombo = new ComboBox<EmailAddressType>(null, targetTypes);
		this.primaryTypeCombo.setWidth("100");
		this.primaryTypeCombo.setSelectedItem(targetTypes.get(0));
		// this.getPrimaryTypeCombo().select(EmailAddressType.To);
		this.grid.addComponent(this.primaryTypeCombo);

		this.primaryTargetAddress = new TextField(null, toEmailAddress);
		this.primaryTargetAddress.setWidth("100%");
		// primaryTargetAddress.setReadOnly(true);
		new Binder<String>().forField(this.primaryTargetAddress)
				.withValidator(new EmailValidator("Please enter a valid email address."));
		// this.getPrimaryTargetAddress().withValidator(new
		// EmailValidator("Please enter a valid email address."));
		this.grid.addComponent(this.primaryTargetAddress);

		this.primaryPlusButton = new Button("+");
		this.primaryPlusButton.setDescription("Click to add another email address line.");
		this.primaryPlusButton.setStyleName(ValoTheme.BUTTON_SMALL);
		this.grid.addComponent(this.primaryPlusButton);
		final Action1<ClickEvent> plusClickAction = new PlusClickAction();
		ButtonEventSource.fromActionOf(this.primaryPlusButton).subscribe(plusClickAction);

		this.subject = new TextField("Subject");
		this.subject.setWidth("100%");

		baseLayout.addComponent(this.subject);
		this.ckEditor = new CKEditorEmailField(false);
		baseLayout.addComponent(this.getCkEditor());
		baseLayout.setExpandRatio(this.getCkEditor(), 1.0f);

		final HorizontalLayout uploadArea = new HorizontalLayout();
		final AbstractLayout uploadWidget = addUploadWidget();
		uploadArea.addComponent(uploadWidget);
		this.attachedFiles = new VerticalLayout();
		final Label attachedLabel = new Label("<b>Attached Files</b>");
		attachedLabel.setContentMode(ContentMode.HTML);
		this.attachedFiles.addComponent(attachedLabel);
		uploadArea.addComponent(this.attachedFiles);
		uploadArea.setWidth("100%");
		uploadArea.setComponentAlignment(uploadWidget, Alignment.TOP_LEFT);
		uploadArea.setComponentAlignment(this.attachedFiles, Alignment.TOP_RIGHT);

		baseLayout.addComponent(uploadArea);

		this.send = new Button("Send");
		this.send.setStyleName(ValoTheme.BUTTON_PRIMARY);
		this.getSend().setDescription("Click to send this email.");
		final Action1<ClickEvent> sendClickAction = new SendClickAction();
		ButtonEventSource.fromActionOf(this.getSend()).subscribe(sendClickAction);

		baseLayout.addComponent(this.getSend());
		baseLayout.setComponentAlignment(this.send, Alignment.BOTTOM_RIGHT);

		this.subject.focus();

	}

	public void show()
	{
		UI.getCurrent().addWindow(this);
		this.setVisible(true);
	}

	List<EmailAddressType> getTargetTypes()
	{
		final ArrayList<EmailAddressType> targetTypes = new ArrayList<>();

		targetTypes.add(EmailAddressType.To);
		targetTypes.add(EmailAddressType.CC);
		targetTypes.add(EmailAddressType.BCC);
		return targetTypes;
	}

	private boolean checkValidAddresses()
	{
		boolean valid = true;
		for (final TargetLine line : this.lines)
		{
			if (!line.targetAddressBinder.validate().isOk())
			{
				valid = false;
				break;
			}
		}
		return valid;
	}

	private boolean isEmpty(final String value)
	{
		return value == null || value.length() == 0;
	}

	private AbstractLayout addUploadWidget()
	{

		final MultiFileUpload multiFileUpload2 = new MultiFileUpload()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void handleFile(final File file, final String fileName, final String mimeType, final long length)
			{
				attachFile(file, true);
			}

			@Override
			protected FileBuffer createReceiver()
			{
				final FileBuffer receiver = super.createReceiver();
				/*
				 * Make receiver not to delete files after they have been
				 * handled by #handleFile().
				 */
				receiver.setDeleteFiles(false);
				return receiver;
			}
		};
		multiFileUpload2.setCaption("Attach files");
		multiFileUpload2.setRootDirectory(EmailForm.TEMP_FILE_DIR);
		return multiFileUpload2;
	}

	/**
	 * @param file
	 * @param deleteAfterSend
	 *            - if true the file will be deleted once the email has been
	 *            sent!!!!!
	 */
	public void attachFile(final File file, boolean deleteAfterSend)
	{
		final HorizontalLayout line = new HorizontalLayout();
		line.setSpacing(true);
		final Button removeButton = new Button("x");

		removeButton.setStyleName("small");

		line.addComponent(removeButton);
		line.addComponent(new Label(file.getName()));
		EmailForm.this.attachedFiles.addComponent(line);

		final AttachedFileLayout attachedFile = new AttachedFileLayout(this.attachedFiles, file, line, deleteAfterSend);
		this.fileList.add(attachedFile);
		removeButton.setData(attachedFile);

		removeButton.addClickListener(l -> {
			final AttachedFileLayout fileToRemove = (AttachedFileLayout) l.getButton().getData();
			fileToRemove.remove();
			EmailForm.this.fileList.remove(fileToRemove);

		});

	}

	private TargetLine insertTargetLine(final int row)
	{
		final List<EmailAddressType> targetTypes = getTargetTypes();

		EmailForm.this.grid.insertRow(row);
		this.grid.setCursorY(row);
		this.grid.setCursorX(0);

		final TargetLine line = new TargetLine();
		line.row = row;

		line.targetTypeCombo = new ComboBox<EmailAddressType>(null, targetTypes);
		line.targetTypeCombo.setWidth("100");
		line.targetTypeCombo.setSelectedItem(targetTypes.get(0));
		this.grid.addComponent(line.targetTypeCombo);

		line.targetAddress = new ComboBox<>(null);
		this.grid.addComponent(line.targetAddress);
		line.targetAddress.setTextInputAllowed(true);
		line.targetAddress.setPlaceholder("Enter Contact Name or email address");
		line.targetAddress.setWidth("100%");

		line.targetAddressBinder = new Binder<Contact>();

		line.targetAddressBinder.forField(line.targetAddress)
				.withValidator(new ContactEmailValidator("Please enter a valid email address."));

		line.targetAddress.setDataProvider(new ListDataProvider<Contact>(this.contactProvider.getAlternateContacts()));
		line.targetAddress.setItemCaptionGenerator(Contact::getFullName);

		// line.targetAddress.setNewItemHandler(inputString ->
		// {
		// final ListDataProvider<Contact> contactProvider =
		// (ListDataProvider<Contact>) line.targetAddress.getDataProvider();
		//
		// Contact contact = new Contact();
		// // contact.se
		// list.add(contact);
		//
		// listDataProvider.refreshItem(contact);
		//
		// line.targetAddress.setSelectedItem(contact);
		// });

		line.minusButton = new Button("-");
		line.minusButton.setDescription("Click to remove this email address line.");
		line.minusButton.setData(line);
		line.minusButton.setStyleName(ValoTheme.BUTTON_SMALL);
		this.grid.addComponent(line.minusButton);
		final Action1<ClickEvent> minusClickAction = new MinusClickAction();

		line.buttonSubscription = ButtonEventSource.fromActionOf(line.minusButton).subscribe(minusClickAction);

		this.lines.add(line);

		return line;
	}

	// private List<Contact> getValidEmailContacts()
	// {
	// return new ContactDao().getByCompany(this.company.getId()).stream()
	// .filter(c -> c.getEmail() != null && c.getEmail().length() >
	// 0).collect(Collectors.toList());
	//
	// }
	//
	// @SuppressWarnings("unchecked")
	// private Item addItem(final ListDataProvider container, final String
	// named, String email)
	// {
	// // When we are editing an email (the second time) we can end up with
	// // double brackets so we strip them off here.
	// if (email.startsWith("<"))
	// {
	// email = email.substring(1);
	// }
	// if (email.endsWith(">"))
	// {
	// email = email.substring(0, email.length() - 1);
	// }
	//
	// final Item item = container.addItem(email);
	// if (item != null)
	// {
	// item.getItemProperty("id").setValue(email);
	// item.getItemProperty("email").setValue(email);
	// String namedEmail;
	// if (named != null && named.trim().length() > 0)
	// {
	// namedEmail = named + " <" + email + ">";
	// }
	// else
	// {
	// namedEmail = "<" + email + ">";
	// }
	// item.getItemProperty("namedemail").setValue(namedEmail);
	// }
	// return item;
	// }

	class PlusClickAction implements Action1<ClickEvent>
	{
		@Override
		public void call(final ClickEvent event)
		{
			final TargetLine newLine = insertTargetLine(EmailForm.this.lines.size() + 1);
			newLine.targetAddress.focus();
		}
	}

	class MinusClickAction implements Action1<ClickEvent>
	{
		@Override
		public void call(final ClickEvent event)
		{
			final Button button = event.getButton();
			final TargetLine line = (TargetLine) button.getData();
			EmailForm.this.grid.removeRow(line.row);
			line.buttonSubscription.unsubscribe();
			EmailForm.this.lines.remove(line.row - 1);

			// recalculate rows
			int row = 1;
			for (final TargetLine aLine : EmailForm.this.lines)
			{
				aLine.row = row++;
			}
		}
	}

	class SendClickAction implements Action1<ClickEvent>, CompleteListener
	{
		@Override
		public void call(final ClickEvent t1)
		{
			EmailForm.this.getSend().setEnabled(false);
			if (isEmpty(EmailForm.this.getSubject()))
			{
				SMNotification.show("The subject may not be blank", Type.WARNING_MESSAGE);
			}
			else if (!checkValidAddresses())
			{
				SMNotification.show("All Email adddresses must be valid", Type.WARNING_MESSAGE);
			}
			else if (isEmpty(EmailForm.this.getCkEditor().getValue()))
			{
				SMNotification.show("The body of the email may not be blank", Type.WARNING_MESSAGE);
			}
			else
			{
				final WorkingDialog working = new WorkingDialog("Sending Email", "Sending...");
				UI.getCurrent().addWindow(working);

				// working.setWorker(runnable, listener);
				working.setWorker(new EmailWorker(UI.getCurrent(), EmailForm.this, EmailForm.this.sender), this);
			}
			EmailForm.this.getSend().setEnabled(true);

		}

		@Override
		public void complete()
		{
			SMNotification.show("Message sent", Type.TRAY_NOTIFICATION);

			for (AttachedFileLayout attachement : EmailForm.this.fileList)
			{
				attachement.remove();
			}
			EmailForm.this.close();
		}

	}

	public HashSet<? extends DataSource> getAttachements()
	{
		HashSet<DataSource> attachements = new HashSet<>();

		for (AttachedFileLayout f : fileList)
		{
			attachements.add(f.getDataSource());

		}
		return attachements;
	}

	public String getPrimaryTargetAddress()
	{
		return primaryTargetAddress.getValue();
	}

	EmailAddressType getPrimaryType()
	{
		return primaryTypeCombo.getValue();
	}

	private CKEditorEmailField getCkEditor()
	{
		return ckEditor;
	}

	UserProvider getSender()
	{
		return sender;
	}

	String getSubject()
	{
		return subject.getValue();
	}

	private Button getSend()
	{
		return send;
	}

	static public class Contact implements Comparable<Contact>
	{

		private String fullName;
		private String email;

		public Contact(String fullName, String email)
		{
			this.fullName = fullName;
			this.email = email;
		}

		public String getFullName()
		{
			return this.fullName;
		}

		public String getEmail()
		{
			return this.email;
		}

		@Override
		public int compareTo(Contact arg0)
		{
			return this.fullName.compareToIgnoreCase(arg0.fullName);
		}

	}

	public void setSubject(String subject)
	{
		this.subject.setValue(subject);

	}

	public void setBody(String body)
	{
		this.body = body;

		String editorBody = this.body;
		if (sender.getEmailSignature() != null)
		{
			editorBody += "</br></br>" + sender.getEmailSignature();
		}

		CKEditorEmailField editor = this.getCkEditor();

		editor.setValue(editorBody);
	}

	public String getBody()
	{
		return this.getCkEditor().getValue();
	}

	public void enableSendButton()
	{
		this.getSend().setEnabled(true);
	}

}