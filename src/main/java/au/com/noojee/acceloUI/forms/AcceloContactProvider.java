package au.com.noojee.acceloUI.forms;

import java.util.List;
import java.util.stream.Collectors;

import au.com.noojee.acceloapi.dao.AffiliationDao;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.ContactDao;
import au.com.noojee.acceloapi.dao.ContractDao;
import au.com.noojee.acceloapi.dao.StaffDao;
import au.com.noojee.acceloapi.entities.Affiliation;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Contact;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.ContractPeriod;
import au.com.noojee.acceloapi.entities.Staff;

public class AcceloContactProvider implements ContactProvider
{

	private Contact defaultContact;
	private List<EmailForm.Contact> alternateContactList;

	public AcceloContactProvider(ContractPeriod contractPeriod)
	{
		Contract contract = new ContractDao().getById(contractPeriod.getContractId());

		Company company = new CompanyDao().getById(contract.getCompanyId());

		Affiliation defaultAffiliate = new AffiliationDao().getById(contract.getBillableAffiliation());
		defaultContact = new ContactDao().getById(defaultAffiliate.getContactId());
		List<Contact> contacts = new ContactDao().getByCompany(company.getId());

		alternateContactList = contacts.stream().map(c -> new EmailForm.Contact(c.getFullName(), c.getEmail())).sorted()
				.filter(c -> c.getEmail() != null && !c.getEmail().isEmpty()).collect(Collectors.toList());

		// We add noojee's staff in to the list as well.
		List<Staff> staff = new StaffDao().getAll();
		alternateContactList.addAll(staff.stream()
				.map(s -> new EmailForm.Contact(s.getFullName(), s.getEmail()))
				.filter(c -> c.getEmail() != null && !c.getEmail().isEmpty())
				.collect(Collectors.toList()));

	}

	@Override
	public EmailForm.Contact getDefaultContact()
	{
		return new EmailForm.Contact(this.defaultContact.getFullName(), this.defaultContact.getEmail());
	}

	@Override
	public List<EmailForm.Contact> getAlternateContacts()
	{
		return this.alternateContactList.stream().map(c -> new EmailForm.Contact(c.getFullName(), c.getEmail()))
				.sorted().collect(Collectors.toList());
	}

}
