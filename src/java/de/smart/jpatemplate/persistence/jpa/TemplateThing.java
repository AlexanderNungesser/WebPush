package de.smart.jpatemplate.persistence.jpa;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This is a class template as example for an jpa entity
 * 
 * @author Florian Fehring
 */
@Entity
@Table(name = "templates", schema = "smartmonitoring", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"title"})})
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "User.count", query = "SELECT COUNT(t) FROM TemplateThing t"),
    @NamedQuery(name = "User.findAll", query = "SELECT t FROM TemplateThing t"),
    @NamedQuery(name = "User.findById", query = "SELECT t FROM TemplateThing t WHERE t.id = :id"),
    @NamedQuery(name = "User.findByTitle", query = "SELECT t FROM TemplateThing t WHERE t.title = :title")
})
public class TemplateThing implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Long id;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String title;
    
    /**
     * Default constructor is needed for JPA manager
     */
    public TemplateThing() {
    }

    /**
     * Default useable construcotr for creation by id
     * 
     * @param id Dataset id
     */
    public TemplateThing(Long id) {
        this.id = id;
    }

    /**
     * Constructs a new TemplateThing
     * 
     * @param id Dateset id
     * @param title Dataset title
     */
    public TemplateThing(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * Copy constructor. Creates a new TemplateThing from a given TemplateThing
     * 
     * @param templatething to copy
     */
    public TemplateThing(TemplateThing templatething) {
        this.id = templatething.getId();
        this.title = templatething.getTitle();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TemplateThing)) {
            return false;
        }
        TemplateThing other = (TemplateThing) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.smart.template.persistence.jpa.TemplateThing[ id=" + id + " ]";
    }
}
