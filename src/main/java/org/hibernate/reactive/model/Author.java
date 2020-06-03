package org.hibernate.reactive.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.PERSIST;

@Entity
@Table(name="authors")
public class Author {
	@Id @GeneratedValue
	Integer id;

	@NotNull @Size(max=100)
	String name;

	@OneToMany(mappedBy = "author", cascade = PERSIST)
	List<Book> books = new ArrayList<>();

	public Author(String name) {
		this.name = name;
	}

	Author() {}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Book> getBooks() {
		return books;
	}

	public void addBook(Book book) {
		books.add(book);
	}
}
                      