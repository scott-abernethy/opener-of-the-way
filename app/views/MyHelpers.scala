package views

import html.helper.FieldConstructor

object MyHelpers {

   implicit val minimal = FieldConstructor(views.html.formfield.f)

}