Java Play! Framework DisplayForModel [![Gittip](http://badgr.co/gittip/fgribreau.png)](https://www.gittip.com/fgribreau/)
====================================

The DisplayForModel template tag returns the HTML markup for each property in the model. 
It can create html forms and supports error validation & model validation. 


Enable the DisplayForModel template tag
---------------------------------------

Copy & Paste the app folder in your project

Restart play (without this step, Play will not recognize the DisplayForModel tag)

Usage
-------

Model:

	package models;
	
	import tags.form.HiddenField;
	
	@Entity
	public class Member extends Model {
		@Email
		@MaxSize(200)
		@Required(message = "member.wrongFormat")
		public String email;

		@HiddenField
		public String password;

		@MaxSize(100)
		public String firstname;

		@MaxSize(100)
		public String lastname;
		
		@IgnoreField
		@ManyToMany
		@JoinTable(name = "MemberFriends")
		public List<Member> friends;
	}
	
The @HiddenField annotation will produce a (input type="hidden") field.

The @IgnoreField annotation will skip the field when rendering the HTML via DisplayForModel.

Controller:

	package controllers;
	
	public class Members extends Controller {
		
		public static void seeProfile(long id) {
			Member memberModel = Member.findById(id);
			
			renderArgs.put("member", memberModel);
			
			render();
		}
		
		public static void editProfile(Member m) {
			
			Member memberModel = m.id != null ? m : Member.find("byEmail", Security.connected()).first();

			renderArgs.put("member", memberModel);

			render();
		}
		
		// On post we receive a "member" object
		public static void editProfilePost(Member member){
			
			// There are errors
			if (validation.hasErrors()) {
				// Redirect to the form
				// DisplayForModel will print errors
				Members.editProfile(member);
			}
			
			// No error, save the member
			member.save();
			
			// Do something else here
		}
	}
	
Views:

_views/Members/editProfil.html_

	#{form @editProfilePost()}
		<!-- In our case the member.id is a field handled by the framework -->
		<!-- So we have to explicitly create this field -->
		<input type="hidden" name="member.id" value="${memberModel.id}" />
		
		#{DisplayForModel model:memberModel /}

		<p>
			<input type="submit" id="save" value="Save" />
		</p>
	#{/form}


_views/Members/seeProfil.html_

	<div class="item">
			#{DisplayForModel model:memberModel, editable:false /}
	</div>

And that's it !
