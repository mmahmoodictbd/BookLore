import {Component, inject, OnInit} from '@angular/core';
import {InputText} from 'primeng/inputtext';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {Checkbox} from 'primeng/checkbox';
import {MultiSelectModule} from 'primeng/multiselect';
import {Library} from '../../../../../book/model/library.model';
import {Button} from 'primeng/button';
import {LibraryService} from '../../../../../book/service/library.service';
import {UserService} from '../../../../../user.service';
import {MessageService} from 'primeng/api';
import {DynamicDialogRef} from 'primeng/dynamicdialog';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-create-user-dialog',
  standalone: true,
  imports: [
    InputText,
    ReactiveFormsModule,
    FormsModule,
    Checkbox,
    MultiSelectModule,
    Button,
    NgIf
  ],
  templateUrl: './create-user-dialog.component.html',
  styleUrl: './create-user-dialog.component.scss'
})
export class CreateUserDialogComponent implements OnInit {
  userForm!: FormGroup;
  libraries: Library[] = [];

  private fb = inject(FormBuilder);
  private libraryService = inject(LibraryService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private ref = inject(DynamicDialogRef);

  ngOnInit() {
    this.libraries = this.libraryService.getLibrariesFromState();

    this.userForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      selectedLibraries: [[], Validators.required],
      canUpload: [false],
      canDownload: [false],
      canEditMetadata: [false]
    });
  }

  createUser() {
    if (this.userForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Please correct errors before submitting.'
      });
      return;
    }

    const userData = this.userForm.value;

    this.userService.createUser(userData).subscribe({
      next: response => {
        this.messageService.add({
          severity: 'success',
          summary: 'User Created',
          detail: response.message
        });
        this.ref.close(true);
      },
      error: err => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to create user: ' + err.error.message
        });
      }
    });
  }
}
