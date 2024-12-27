import {Component, computed, OnInit} from '@angular/core';
import {AppMenuitemComponent} from './app.menuitem.component';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {MenuModule} from 'primeng/menu';
import {LibraryService} from '../../service/library.service';
import {async, Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {ShelfService} from '../../service/shelf.service';

@Component({
  selector: 'app-menu',
  imports: [AppMenuitemComponent, NgIf, NgForOf, MenuModule, AsyncPipe],
  templateUrl: './app.menu.component.html',
})
export class AppMenuComponent implements OnInit {
  home: any[] = [];
  libraryMenu$: Observable<any> | undefined;
  shelfMenu$: Observable<any> | undefined;

  constructor(private libraryService: LibraryService, private shelfService: ShelfService) {
  }

  ngOnInit() {
    this.libraryMenu$ = this.libraryService.libraries$.pipe(
      map((libraries) => [
        {
          label: 'Library',
          separator: false,
          items: libraries.map((library) => ({
            label: library.name,
            icon: 'pi pi-' + library.icon,
            routerLink: [`/library/${library.id}/books`],
            bookCount$: this.libraryService.getBookCount(library.id ?? 0),
          })),
        },
      ])
    );

    this.shelfMenu$ = this.shelfService.shelves$.pipe(
      map((shelves) => [
        {
          label: 'Shelves',
          separator: false,
          items: shelves.map((shelf) => ({
            label: shelf.name,
            icon: 'pi pi-' + shelf.icon,
            routerLink: [`/shelf/${shelf.id}/books`],
            bookCount$: this.shelfService.getBookCount(shelf.id ?? 0),
          })),
        },
      ])
    );

    this.populateHome();
  }

  populateHome() {
    this.home = [
      {
        label: 'Home',
        items: [
          {label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/']}
        ],
      },
    ];
  }

}
