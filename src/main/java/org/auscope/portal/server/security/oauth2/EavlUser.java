/**
 *
 */
package org.auscope.portal.server.security.oauth2;

import java.util.Collection;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author fri096
 *
 */
@Entity
public class EavlUser implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 5483133230772661314L;

    @Id
    private String username;

    @OneToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval = true)
    private Set<EAVLAuthority> authorities;

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param authorities the authorities to set
     */
    public void setAuthorities(Set<EAVLAuthority> authorities) {
        this.authorities = authorities;
    }

    /**
     * @return the fullName
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    private String email;

    @Basic
    private String fullName;

    @Override
    public void eraseCredentials() {
        // Empty
    }

    public EavlUser() {
        // JPA only;
    }

    public EavlUser(String username) {
        this.username = username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setEmail(String email) {
        this.email=email;
    }

    public void setFullName(String string) {
        this.fullName = string;

    }

}
