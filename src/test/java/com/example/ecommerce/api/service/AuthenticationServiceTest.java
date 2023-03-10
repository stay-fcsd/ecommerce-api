package com.example.ecommerce.api.service;

import com.example.ecommerce.api.config.JwtService;
import com.example.ecommerce.api.entity.UserRole;
import com.example.ecommerce.api.mapstruct.dto.user.AuthenticationRequestDto;
import com.example.ecommerce.api.mapstruct.dto.user.RegistrationRequestDto;
import com.example.ecommerce.api.entity.User;
import com.example.ecommerce.api.exception.ConflictException;
import com.example.ecommerce.api.exception.ExceptionMessages;
import com.example.ecommerce.api.exception.InvalidCredentialsException;
import com.example.ecommerce.api.mapstruct.mappers.UserMapper;
import com.example.ecommerce.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    UserMapper userMapper;
    @InjectMocks
    private AuthenticationService cut;
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    private static final String EMAIL = "john@gmail.com";
    private static final String PASSWORD = "12345678";

    @Test
    void shouldRegisterUserWhenEmailIsUnique() {
        // Given
        RegistrationRequestDto request = getRegistrationRequest();
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(UserRole.USER)
                .build();


        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(userMapper.mapRegistrationRequestDtoToUser(request)).willReturn(user);

        // When
        cut.register(request);

        // Then
        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo(request.getFirstName());
    }

    @Test
    void shouldFailUserRegistrationWhenEmailIsNotUnique() {
       // Given
       RegistrationRequestDto request = getRegistrationRequest();

       given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(new User()));

       // When
        assertThatThrownBy(() -> cut.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ExceptionMessages.USER_TAKEN);

        // Then
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldFailUserRegistrationWhenPasswordsDoNotMatch() {
        // Given
        RegistrationRequestDto request = getRegistrationRequest();
        request.setVerifyPassword("23434");

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());


        // When
        assertThatThrownBy(() -> cut.register(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(ExceptionMessages.PASSWORDS_MUST_MATCH);

        // Then
        then(userRepository).should(never()).save(any());
    }

    @Test
    void shouldAuthenticateUserWhenCredentialsAreCorrect() {
        // Given
        AuthenticationRequestDto request = getAuthenticationRequest();
        User user = User.builder()
                        .id(1L)
                        .password("12345678")
                        .email("john@gmail.com")
                        .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(true);

        // When
        cut.authenticate(request);

        // Then
        then(jwtService).should(times(1)).generateToken(user);
    }

    @Test
    void shouldFailAuthenticationWhenUserWithEmailDoesNotExist() {
        // Given
        AuthenticationRequestDto request = getAuthenticationRequest();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> cut.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(ExceptionMessages.INVALID_CREDENTIALS);

        // Then
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void shouldFailAuthenticationWhenPasswordIsIncorrect() {
        // Given
        AuthenticationRequestDto request = getAuthenticationRequest();
        User user = User.builder()
                        .password("234234")
                        .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

        // When
        assertThatThrownBy(() -> cut.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(ExceptionMessages.INVALID_CREDENTIALS);

        // Then
        then(jwtService).shouldHaveNoInteractions();

    }

    private AuthenticationRequestDto getAuthenticationRequest() {
        return AuthenticationRequestDto.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();
    }

    private RegistrationRequestDto getRegistrationRequest() {
        return RegistrationRequestDto.builder()
                .firstName("John")
                .lastName("Last")
                .email(EMAIL)
                .password(PASSWORD)
                .verifyPassword(PASSWORD)
                .build();
    }
}