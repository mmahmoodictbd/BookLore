import {Component, Signal} from '@angular/core';
import {Library} from '../../model/library.model';
import {Button} from 'primeng/button';
import {NgIf} from '@angular/common';
import {LibraryCreatorComponent} from '../library-creator/library-creator.component';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {DashboardScrollerComponent} from '../dashboard-scroller/dashboard-scroller.component';
import {LibraryAndBookService} from '../../service/library-and-book.service';

@Component({
  selector: 'app-home-page',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    Button,
    NgIf,
    DashboardScrollerComponent
  ],
  providers: [DialogService],
})
export class DashboardComponent {
  private libraries: Signal<Library[]>;
  ref: DynamicDialogRef | undefined;

  constructor(private libraryBookService: LibraryAndBookService, public dialogService: DialogService) {
    this.libraries = this.libraryBookService.getLibraries();
  }

  get isLibrariesEmpty(): boolean {
    return this.libraries()?.length === 0;
  }

  createNewLibrary(event: MouseEvent) {
    const buttonRect = (event.target as HTMLElement).getBoundingClientRect();
    const dialogWidthPercentage = 50;
    const viewportWidth = window.innerWidth;
    const dialogWidth = (dialogWidthPercentage / 100) * viewportWidth;
    const leftPosition = buttonRect.left + (buttonRect.width / 2) - (dialogWidth / 2);
    this.ref = this.dialogService.open(LibraryCreatorComponent, {
      modal: true,
      width: `${dialogWidthPercentage}%`,
      height: 'auto',
      style: {
        position: 'absolute',
        top: `${buttonRect.bottom + 10}px`,
        left: `${Math.max(leftPosition, 0)}px`
      },
    });
  }
}
