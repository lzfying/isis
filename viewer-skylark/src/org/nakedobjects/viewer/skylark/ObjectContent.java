package org.nakedobjects.viewer.skylark;

import org.nakedobjects.object.ApplicationContext;
import org.nakedobjects.object.InvalidEntryException;
import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedClass;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.control.AbstractConsent;
import org.nakedobjects.object.control.Allow;
import org.nakedobjects.object.control.Consent;
import org.nakedobjects.object.control.Hint;
import org.nakedobjects.object.control.Veto;
import org.nakedobjects.object.reflect.Action;
import org.nakedobjects.object.reflect.NakedObjectAssociation;
import org.nakedobjects.object.reflect.NakedObjectField;
import org.nakedobjects.utility.Assert;
import org.nakedobjects.utility.UnexpectedCallException;
import org.nakedobjects.viewer.skylark.basic.AbstractContent;
import org.nakedobjects.viewer.skylark.basic.ClassOption;
import org.nakedobjects.viewer.skylark.basic.ObjectOption;
import org.nakedobjects.viewer.skylark.util.ImageFactory;


public abstract class ObjectContent extends AbstractContent {

    public abstract Consent canClear();

    public Consent canDrop(Content sourceContent) {
        if (!(sourceContent instanceof ObjectContent)) {
            return new Veto("Can't drop " + sourceContent.getNaked());
        } else {
            NakedObject source = ((ObjectContent) sourceContent).getObject();
            NakedObject target = (NakedObject) getObject();

            Action action = dropAction(source, target);
            if (action != null) {
                Hint about = target.getHint(action, new NakedObject[] { source });
                return about.canUse();

            } else {
                if (target.getOid() != null && source.getOid() == null) {
                    return new Veto("Can't set field in persistent object with reference to non-persistent object");

                } else {
                    NakedObjectField[] fields = target.getVisibleFields();
                    for (int i = 0; i < fields.length; i++) {
                        if (source.getSpecification().isOfType(fields[i].getSpecification())) {
                            if (target.getField(fields[i]) == null) {
                                return new Allow("Set field " + target.getLabel(fields[i]));
                            }
                        }
                    }
                    return new Veto("No empty field accepting object of type " + source.getSpecification().getSingularName());
                }
            }
        }
    }

    public abstract Consent canSet(NakedObject dragSource);

    public abstract void clear();

    public Naked drop(Content sourceContent) {
        NakedObject source = (NakedObject) sourceContent.getNaked();
        Assert.assertNotNull(source);

        NakedObject target = (NakedObject) getObject();
        Assert.assertNotNull(target);

        if (canDrop(sourceContent).isAllowed()) {
            Action action = dropAction(source, target);

            if ((action != null) && target.getHint(action, new NakedObject[] { source }).canUse().isAllowed()) {
                return target.execute(action, new NakedObject[] { source });

            } else {
                NakedObjectField[] fields = target.getVisibleFields();
                for (int i = 0; i < fields.length; i++) {
                    if (source.getSpecification().isOfType(fields[i].getSpecification()) && target.getField(fields[i]) == null) {
                        target.setAssociation(((NakedObjectAssociation) fields[i]), source);
                        //           invalidateContent();
                        break;
                    }
                }
            }
        }
        return null;
    }

    private Action dropAction(NakedObject source, NakedObject target) {
        Action action;
        if (target.getObject() instanceof NakedClass) {
            NakedObjectSpecification forNakedClass = ((NakedClass) target.getObject()).forObjectType();
            action = forNakedClass
                    .getClassAction(Action.USER, null, new NakedObjectSpecification[] { source.getSpecification() });
        } else {
            action = target.getSpecification().getObjectAction(Action.USER, null,
                    new NakedObjectSpecification[] { source.getSpecification() });
        }
        return action;
    }

    public Hint getHint() {
        return null;
    }

    public abstract NakedObject getObject();

    public void menuOptions(MenuOptionSet options) {
        final NakedObject object = getObject();
        ObjectOption.menuOptions(object, options);

        if (getObject() == null) {
            ClassOption.menuOptions(getSpecification(), options);
        }

        if (object instanceof ApplicationContext) {
            options.add(MenuOptionSet.VIEW, new MenuOption("New Workspace") {
                public Consent disabled(View component) {
                    return AbstractConsent.allow(object instanceof ApplicationContext);
                }

                public void execute(Workspace workspace, View view, Location at) {
                    View newWorkspace;
                    Content content = Skylark.getContentFactory().createRootContent(object);
                    newWorkspace = Skylark.getViewFactory().createInnerWorkspace(content);
                    newWorkspace.setLocation(at);
                    workspace.addView(newWorkspace);
                    newWorkspace.markDamaged();
                }
            });
        }

        options.add(MenuOptionSet.DEBUG, new MenuOption("Class...") {
            public Consent disabled(View component) {
                return AbstractConsent.allow(object != null);
            }

            public void execute(Workspace workspace, View view, Location at) {
            /*
             * TODO reimplement return
             * getObjectManager().getNakedClass(getObject().getSpecification());
             */
            }
        });

        options.add(MenuOptionSet.DEBUG, new MenuOption("Clone...") {
            public Consent disabled(View component) {
                return AbstractConsent.allow(object != null);
            }

            public void execute(Workspace workspace, View view, Location at) {
            /*
             * TODO reimplement AbstractNakedObject clone =
             * (AbstractNakedObject) createInstance(getClass());
             * clone.copyObject(this); clone.objectChanged();
             * 
             * Skylark.getViewFactory().createInnerWorkspace(clone);
             * newWorkspace.setLocation(at);
             * getWorkspace().addView(newWorkspace); newWorkspace.markDamaged();
             */
            }
        });

        options.add(MenuOptionSet.DEBUG, new MenuOption("Clear resolved") {
            public Consent disabled(View component) {
                return AbstractConsent.allow(object != null && !object.getResolveState().isResolved());
            }

            public void execute(Workspace workspace, View view, Location at) {
                object.debugClearResolved();
            }
        });

    }

    public void parseTextEntry(String entryText) throws InvalidEntryException {
        throw new UnexpectedCallException();
    }

    public abstract void setObject(NakedObject object);
    
    public String getIconName() {
        NakedObject object = getObject();
        return object == null ? null : object.getIconName();
    }
    
    public Image getIconPicture(int iconHeight) {
        NakedObject nakedObject = getObject();
        Object object = nakedObject.getObject();
        if( object instanceof NakedClass) {
            NakedObjectSpecification specification = ((NakedClass) object).forObjectType();
            return ImageFactory.getInstance().loadClassIcon(specification, "", iconHeight);
        } else {
	        NakedObjectSpecification specification = nakedObject.getSpecification();
	        return ImageFactory.getInstance().loadObjectIcon(specification, "", iconHeight);
        }
    }

}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2005 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */