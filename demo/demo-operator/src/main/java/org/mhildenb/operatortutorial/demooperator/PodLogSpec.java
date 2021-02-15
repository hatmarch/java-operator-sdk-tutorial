package org.mhildenb.operatortutorial.demooperator;

public class PodLogSpec {
    static public PodLogSpec createFromName(String name)
    {
        var newSpec = new PodLogSpec();
        newSpec.name = name;
        return newSpec;
    }

    // Depends only on the name of the customservice
    @Override
    public int hashCode() 
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) 
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        // for our purposes, if the customservice shares a name, consider them equal
        return (((PodLogSpec) o).name.equals(this.name));
    }

    public String name;
    public Boolean elevatedLogging;
}
