import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {Divider} from 'primeng/divider';
import {NgForOf, NgIf} from '@angular/common';
import {Provider} from '../../book/model/provider.model';
import {BookService} from '../../book/service/book.service';
import {FetchMetadataRequest} from '../../book/model/fetch-metadata-request.model';
import {FetchedMetadata} from '../../book/model/book.model';
import {ProgressSpinner} from 'primeng/progressspinner';
import {MetadataPickerComponent} from '../metadata-picker/metadata-picker.component';
import {BookMetadataCenterService} from '../book-metadata-center.service';
import {MultiSelect} from 'primeng/multiselect';

@Component({
  selector: 'app-metadata-searcher',
  templateUrl: './metadata-searcher.component.html',
  styleUrls: ['./metadata-searcher.component.scss'],
  imports: [
    ReactiveFormsModule,
    Button,
    InputText,
    Divider,
    NgForOf,
    NgIf,
    ProgressSpinner,
    MetadataPickerComponent,
    MultiSelect
  ],
  standalone: true
})
export class MetadataSearcherComponent implements OnInit {

  form: FormGroup;
  providers = Object.values(Provider);
  allFetchedMetadata: FetchedMetadata[] = [];
  selectedFetchedMetadata!: FetchedMetadata | null;
  bookId!: number;
  loading: boolean = false;

  constructor(private fb: FormBuilder, private bookService: BookService, private bookInfoService: BookMetadataCenterService) {
    this.form = this.fb.group({
      provider: null,
      isbn: [''],
      title: [''],
      author: [''],
    });
  }

  ngOnInit() {
    this.bookInfoService.bookMetadata$.subscribe((bookMetadata => {
      if (bookMetadata) {
        this.bookId = bookMetadata.bookId;
        this.form.setValue(({
          provider: null,
          isbn: bookMetadata.isbn10,
          title: bookMetadata.title,
          author: bookMetadata.authors.length > 0 ? bookMetadata.authors[0] : ''
        }))
      }
    }))
  }

  get isSearchEnabled(): boolean {
    const providerSelected = !!this.form.get('provider')?.value;
    const isbnOrTitle = this.form.get('isbn')?.value || this.form.get('title')?.value;
    return providerSelected && isbnOrTitle;
  }

  onSubmit(): void {
    if (this.form.valid) {
      const providerKeys = Object.keys(Provider).filter(key =>
        (this.form.get('provider')?.value as string[]).includes(Provider[key as keyof typeof Provider])
      );
      if (!providerKeys) {
        console.error('Invalid provider selected.');
        return;
      }
      const fetchRequest: FetchMetadataRequest = {
        bookId: this.bookId,
        providers: providerKeys,
        title: this.form.get('title')?.value,
        isbn: this.form.get('isbn')?.value,
        author: this.form.get('author')?.value
      };
      this.loading = true;
      this.bookService.fetchMetadata(fetchRequest.bookId, fetchRequest)
        .subscribe({
          next: (fetchedMetadata) => {
            this.loading = false;
            this.allFetchedMetadata = fetchedMetadata.map((book) => ({
              ...book,
              thumbnailUrl: book.thumbnailUrl
            }));
          },
          error: (err) => {
            this.loading = false;
          }
        });
    } else {
      console.warn('Form is invalid. Please fill in all required fields.');
    }
  }

  buildProviderLink(metadata: { provider: string; providerBookId: string }): string {
    if (!metadata.provider || !metadata.providerBookId) {
      throw new Error("Invalid metadata: 'provider' or 'providerBookId' is missing.");
    }
    switch (metadata.provider) {
      case "AMAZON":
        return `<a href="https://www.amazon.com/dp/${metadata.providerBookId}" target="_blank">Amazon</a>`;
      case "GOOD_READS":
        return `<a href="https://www.goodreads.com/book/show/${metadata.providerBookId}" target="_blank">Goodreads</a>`;
      default:
        throw new Error(`Unsupported provider: ${metadata.provider}`);
    }
  }

  truncateText(text: string | null, length: number): string {
    const safeText = text ?? '';
    return safeText.length > length ? safeText.substring(0, length) + '...' : safeText;
  }

  onBookClick(fetchedMetadata: FetchedMetadata) {
    this.selectedFetchedMetadata = fetchedMetadata;
  }

  onGoBack() {
    this.selectedFetchedMetadata = null;
  }

  sanitizeHtml(htmlString: string | null | undefined): string {
    if (!htmlString) {
      return '';
    }
    return htmlString.replace(/<\/?[^>]+(>|$)/g, '').trim();
  }
}
