/* Project: play-framework-form
 * Package: tags.form
 * File   : DisplayForModel
 * Created: Apr 28, 2011 - 7:27:42 PM
 *
 *
 * Copyright 2011 Francois-Guillaume Ribreau
 *
 * Based on HTML5-Validation
 * see: http://bit.ly/mHdUux 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Usage:
 * 
 * Render the Model instance myModel
 * 		#{DisplayForModel model:myModel /}
 * 
 * Render a Model without myModel.field1 and myModel.field2
 * 		#{DisplayForModel model:myModel, ignore:'field1, field2' /}
 * 
 * Render a Model in view mode only
 *		#{DisplayForModel model:myModel, editable:false /}
 */

package tags.form;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.Max;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.Password;
import play.data.validation.Range;
import play.data.validation.Required;
import play.data.validation.URL;
import play.data.validation.Validation;
import play.db.Model;
import play.i18n.Messages;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;

public class DisplayForModel extends FastTags {

	/**
	 * Returns HTML markup for each property in the model
	 * 
	 * This method is typically used to display values from the object that is
	 * exposed by the Model property.
	 * 
	 * @param o
	 * @return The HTML markup for each property in the model.
	 * @throws Exception
	 */
	public static void _DisplayForModel(Map<?, ?> args, Closure body,
			PrintWriter out, ExecutableTemplate template, int fromLine)
			throws NullPointerException, ClassNotFoundException,
			IllegalAccessException, NoSuchFieldException {

		// Parameters
		Model model = getModel(args);
		List<String> ignores = getIgnores(args);
		Boolean editable = getEditable(args);

		Class<?> clazz = model.getClass();

		String modelName = clazz.getName()
				.substring(clazz.getName().lastIndexOf(".") + 1).toLowerCase();

		Field[] fields = clazz.getDeclaredFields();

		for (Field field : fields) {

			if ((ignores == null && !field
					.isAnnotationPresent(IgnoreField.class))
					|| (ignores != null && !ignores.contains(field.getName()))) {

				String fieldName = modelName + "." + field.getName();
				Object fieldValue = Class.forName(field.getType().getName())
						.cast(clazz.getField(field.getName()).get(model));

				if (!field.isAnnotationPresent(HiddenField.class)) {

					if (editable) {
						renderFieldEditable(out, field, fieldName, fieldValue);
					} else {
						renderFieldView(out, field, fieldName, fieldValue);
					}

				} else {// Hidden
					out.print("<input type='hidden' name='" + fieldName
							+ "' value='" + fieldValue + "'/>");
				}
			}
		}

	}

	private static void renderFieldView(PrintWriter out, Field field,
			String fieldName, Object fieldValue) {

		out.print("\n<p>\n\t<label for='" + fieldName.replace(".", "") + "'>"
				+ Messages.get(fieldName) + "</label>\n");

		out.print("\t<span id='" + fieldName.replace(".", "") + "' name='"
				+ fieldName + "'");

		out.print("/>\n");

		if (fieldValue != null && !fieldValue.toString().isEmpty()) {
			out.print(fieldValue);
		} else {
			out.print("/");
		}

		out.print("</span>");

		printErrorIfNeeded(out, fieldName);

		out.print("</p>\n");
	}

	private static void renderFieldEditable(PrintWriter out, Field field,
			String fieldName, Object fieldValue) {

		out.print("\n<p>\n\t<label for='" + fieldName.replace(".", "") + "'>"
				+ Messages.get(fieldName) + "</label>\n\t");

		out.print("<input id='" + fieldName.replace(".", "") + "' name='"
				+ fieldName + "'");

		if (fieldValue != null) {
			out.print("value='" + fieldValue + "'");
		}

		printValidationAttributes(out, field, fieldName);

		out.print("/>\n");

		printErrorIfNeeded(out, fieldName);

		out.print("</p>\n");
	}

	/**
	 * Affiche les erreurs d'un champ si besoin
	 */
	private static void printErrorIfNeeded(PrintWriter out, String fieldName) {
		if (Validation.hasError(fieldName)) {

			play.data.validation.Error error = Validation.error(fieldName);

			if (error != null && !error.message().isEmpty()) {
				out.print("<span class='error'>");
				out.print(error.message());

				out.print("</span>");
			}

		}
	}

	/**
	 * Prints validation attributes for a given field.
	 * 
	 * @param args
	 *            The tag attributes.
	 * @param out
	 *            The print writer to use.
	 * @param fieldName
	 * @param fieldValue
	 * @throws SecurityException
	 *             Thrown when either the field or the getter for the field
	 *             can't be reached.
	 * @throws NoSuchFieldException
	 *             Thrown when the field can't be reached.
	 * @throws ClassNotFoundException
	 *             Thrown when the class could not be found.
	 */
	private static void printValidationAttributes(PrintWriter out, Field field,
			String fieldName) {

		// Ajout du type
		if (field.isAnnotationPresent(URL.class)) {
			printAttribute("type", "url", out);
			printAttribute("placeholder", "http://domain.com", out);

		} else if (field.isAnnotationPresent(Email.class)) {
			printAttribute("type", "email", out);
			printAttribute("placeholder", "email@domain.com", out);

		} else if (field.isAnnotationPresent(Password.class)) {
			printAttribute("type", "password", out);
			printAttribute("placeholder", "password", out);

		} else if (field.getType().getName().contains("Date")) {
			printAttribute("type", "date", out);

		} else {
			printAttribute("type", "text", out);
			printAttribute("placeholder", Messages.get(fieldName), out);
		}

		// Champs supplémentaire
		if (field.isAnnotationPresent(Required.class)) {
			printAttribute("required", "required", out);
		}

		if (field.isAnnotationPresent(Min.class)) {
			final Min min = field.getAnnotation(Min.class);
			printAttribute("min", String.valueOf(min.value()), out);
		}

		if (field.isAnnotationPresent(Max.class)) {
			final Max max = field.getAnnotation(Max.class);
			printAttribute("max", String.valueOf(max.value()), out);
		}

		if (field.isAnnotationPresent(Range.class)) {
			final Range range = field.getAnnotation(Range.class);
			printAttribute("min", String.valueOf(range.min()), out);
			printAttribute("max", String.valueOf(range.max()), out);
		}

		if (field.isAnnotationPresent(MaxSize.class)) {
			final MaxSize maxSize = field.getAnnotation(MaxSize.class);
			printAttribute("maxlength", String.valueOf(maxSize.value()), out);
		}

		if (field.isAnnotationPresent(Match.class)) {
			final Match match = field.getAnnotation(Match.class);
			printAttribute("pattern", match.value(), out);
		}
	}

	/**
	 * Prints a single attribute using a given print writer.
	 * 
	 * If <code>null</code> is given as value nothing will be printed to
	 * eliminate empty attributes.
	 * 
	 * @param name
	 *            The name of the attribute to print.
	 * @param value
	 *            The value of the attribute to print (may be <code>null</code>
	 *            ).
	 * @param out
	 *            The print writer to use.
	 */
	private static void printAttribute(final String name, final Object value,
			final PrintWriter out) {
		if (value != null) {
			out.print(" " + name + "=\"" + value + "\"");
		}
	}

	/**
	 * Get the editable parameter
	 * 
	 * @param args
	 * @return
	 */
	private static Boolean getEditable(Map<?, ?> args) {
		return (args.get("editable") == null || (args.get("editable") != null && args
				.get("editable").equals(true)));
	}

	/**
	 * Get the ignores parameter
	 * 
	 * @param args
	 * @return
	 */
	private static List<String> getIgnores(Map<?, ?> args) {
		List<String> ignores = null;

		if (args.get("ignore") != null) {
			ignores = Arrays.asList(((String) args.get("ignore")).replaceAll(
					" ", "").split(","));
		}

		return ignores;
	}

	/**
	 * Get the model parameter
	 * 
	 * @param args
	 * @return
	 * @throws NullPointerException
	 */
	private static Model getModel(Map<?, ?> args) throws NullPointerException {
		Model m = (Model) args.get("model");

		if (m == null) {
			throw new NullPointerException(
					"You must specify a model. \nE.g: #{DisplayForModel model:yourModelInstance /}");
		}

		return m;
	}
}
